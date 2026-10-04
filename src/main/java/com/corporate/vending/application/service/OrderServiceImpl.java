package com.corporate.vending.application.service;

import com.corporate.vending.adapter.out.persistence.MachineRepository;
import com.corporate.vending.adapter.out.persistence.OrderRepository;
import com.corporate.vending.adapter.out.persistence.OrderStateAuditRepository;
import com.corporate.vending.adapter.out.persistence.PaymentRepository;
import com.corporate.vending.adapter.out.persistence.RfidBadgeRepository;
import com.corporate.vending.adapter.out.persistence.SlotRepository;
import com.corporate.vending.application.dto.CreateOrderRequest;
import com.corporate.vending.application.dto.OrderResponse;
import com.corporate.vending.application.dto.PayRequest;
import com.corporate.vending.application.dto.PaymentResponse;
import com.corporate.vending.application.payment.PaymentRequest;
import com.corporate.vending.application.payment.PaymentResult;
import com.corporate.vending.application.statemachine.OrderStateMachineService;
import com.corporate.vending.domain.MachineStatus;
import com.corporate.vending.domain.PaymentMethod;
import com.corporate.vending.domain.PaymentStatus;
import com.corporate.vending.domain.SlotStatus;
import com.corporate.vending.domain.VendingEvent;
import com.corporate.vending.domain.VendingState;
import com.corporate.vending.domain.exception.InvalidStateException;
import com.corporate.vending.domain.exception.ResourceNotFoundException;
import com.corporate.vending.domain.exception.SlotUnavailableException;
import com.corporate.vending.domain.model.Machine;
import com.corporate.vending.domain.model.OrderEntity;
import com.corporate.vending.domain.model.OrderStateAudit;
import com.corporate.vending.domain.model.PaymentEntity;
import com.corporate.vending.domain.model.Slot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orders;
    private final MachineRepository machines;
    private final SlotRepository slots;
    private final PaymentRepository payments;
    private final RfidBadgeRepository badges;
    private final OrderStateAuditRepository audits;
    private final OrderStateMachineService stateMachines;
    private final PaymentService paymentService;
    private final InventoryService inventory;
    private final RefundHandler refundHandler;
    private final OrderMapper mapper;

    @Override
    @Transactional
    public OrderResponse createOrder(String idempotencyKey, CreateOrderRequest request) {
        Optional<OrderEntity> existing = orders.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return mapper.toResponse(existing.get());
        }
        Machine machine = machines.findById(request.machineId())
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found: " + request.machineId()));
        if (machine.getStatus() != MachineStatus.ONLINE) {
            throw new SlotUnavailableException("Machine is not available: " + machine.getStatus());
        }
        Slot slot = slots.findByMachineIdAndSlotNo(machine.getId(), request.slotNo())
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + request.slotNo()));
        if (slot.getStatus() != SlotStatus.ACTIVE || slot.getProduct() == null) {
            throw new SlotUnavailableException("Slot is not available: " + request.slotNo());
        }

        OrderEntity order = new OrderEntity();
        order.setIdempotencyKey(idempotencyKey);
        order.setMachine(machine);
        order.setSlot(slot);
        order.setProduct(slot.getProduct());
        order.setAmountPaise(slot.getProduct().getPricePaise());
        order.setState(VendingState.IDLE);
        order = orders.saveAndFlush(order);

        try {
            transition(order, VendingEvent.SELECT_ITEM);
        } catch (InvalidStateException e) {
            throw new SlotUnavailableException("Slot is not available: " + request.slotNo());
        }
        transition(order, VendingEvent.INITIATE_PAYMENT);
        return mapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        return mapper.toResponse(load(orderId));
    }

    @Override
    @Transactional
    public PaymentResponse pay(UUID orderId, PayRequest request) {
        OrderEntity order = load(orderId);
        if (order.getState() != VendingState.PAYMENT_PENDING) {
            throw new InvalidStateException("Order is not awaiting payment (state " + order.getState() + ")");
        }
        if (payments.existsByOrderIdAndStatusIn(orderId, List.of(PaymentStatus.INITIATED, PaymentStatus.SUCCESS))) {
            throw new InvalidStateException("A payment is already in progress for this order");
        }
        if (request.method() == PaymentMethod.RFID && request.badgeHash() != null) {
            badges.findByBadgeHash(request.badgeHash()).ifPresent(b -> order.setEmployee(b.getEmployee()));
        }

        PaymentResult result = paymentService.pay(new PaymentRequest(orderId, request.method(), order.getAmountPaise(),
                request.badgeHash(), request.cardToken(), request.payerVpa(), null));

        PaymentEntity payment = new PaymentEntity();
        payment.setOrder(order);
        payment.setMethod(request.method());
        payment.setAmountPaise(order.getAmountPaise());
        payment.setGatewayTxnId(result.gatewayTxnId());

        switch (result.status()) {
            case SUCCESS -> {
                payment.setStatus(PaymentStatus.SUCCESS);
                payments.save(payment);
                transition(order, VendingEvent.PAYMENT_SUCCESS);
            }
            case PENDING -> {
                payment.setStatus(PaymentStatus.INITIATED);
                payments.save(payment);
            }
            case FAILED -> {
                payment.setStatus(PaymentStatus.FAILED);
                payments.save(payment);
                transition(order, VendingEvent.PAYMENT_FAILED);
            }
        }
        return new PaymentResponse(result.status().name(), order.getState(), result.qrPayload(), result.message());
    }

    @Override
    @Transactional
    public void onUpiPaymentResult(UUID orderId, String gatewayTxnId, boolean success) {
        PaymentEntity payment = payments.findByGatewayTxnId(gatewayTxnId)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown payment transaction: " + gatewayTxnId));
        if (!payment.getOrder().getId().equals(orderId)) {
            throw new InvalidStateException("Transaction does not belong to order " + orderId);
        }
        if (payment.getStatus() != PaymentStatus.INITIATED) {
            log.info("Ignoring duplicate webhook for txn {} (status {})", gatewayTxnId, payment.getStatus());
            return;
        }
        OrderEntity order = payment.getOrder();
        if (!success) {
            payment.setStatus(PaymentStatus.FAILED);
            payments.save(payment);
            if (order.getState() == VendingState.PAYMENT_PENDING) {
                transition(order, VendingEvent.PAYMENT_FAILED);
            }
            return;
        }
        payment.setStatus(PaymentStatus.SUCCESS);
        payments.save(payment);
        if (order.getState() == VendingState.PAYMENT_PENDING) {
            transition(order, VendingEvent.PAYMENT_SUCCESS);
        } else {
            log.warn("Late UPI payment for order {} in state {}; refunding", orderId, order.getState());
            refundHandler.refund(orderId);
        }
    }

    @Override
    @Transactional
    public void onDropResult(UUID orderId, boolean success) {
        OrderEntity order = orders.findById(orderId).orElse(null);
        if (order == null || order.getState() != VendingState.DISPENSING) {
            log.info("Ignoring drop result for order {} (not dispensing)", orderId);
            return;
        }
        UUID machineId = order.getMachine().getId();
        UUID slotId = order.getSlot().getId();
        String slotNo = order.getSlot().getSlotNo();

        if (success) {
            slots.decrementQuantity(slotId);
            slots.resetFaultCount(slotId);
            transition(order, VendingEvent.DROP_CONFIRMED);
            inventory.complete(machineId, slotNo, orderId);
            return;
        }

        boolean slotFaulty = inventory.recordFault(machineId, slotNo);
        slots.incrementFaultCount(slotId);
        if (slotFaulty) {
            slots.updateStatus(slotId, SlotStatus.FAULTY);
        }
        transition(order, VendingEvent.DROP_FAILED);
        transition(order, VendingEvent.REFUND_DONE);
        if (!inventory.hasAnyActiveSlot(machineId)) {
            machines.updateStatus(machineId, MachineStatus.OUT_OF_SERVICE);
            log.error("Machine {} has no active slots left; marked OUT_OF_SERVICE", machineId);
        }
    }

    @Override
    @Transactional
    public void expireOrder(UUID orderId) {
        OrderEntity order = orders.findById(orderId).orElse(null);
        if (order == null || order.getState() != VendingState.PAYMENT_PENDING) {
            return;
        }
        transition(order, VendingEvent.TIMEOUT);
    }

    private OrderEntity load(UUID orderId) {
        return orders.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    private void transition(OrderEntity order, VendingEvent event) {
        VendingState from = order.getState();
        VendingState to = stateMachines.fire(order.getId(), order.getMachine().getId(),
                order.getSlot().getSlotNo(), from, event);
        if (to == from) {
            throw new InvalidStateException("Event " + event + " is not allowed in state " + from);
        }
        order.setState(to);
        orders.save(order);
        audits.save(new OrderStateAudit(order.getId(), from.name(), to.name(), event.name()));
    }
}
