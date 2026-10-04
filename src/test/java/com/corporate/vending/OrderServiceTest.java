package com.corporate.vending;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import com.corporate.vending.application.payment.PaymentResult;
import com.corporate.vending.application.service.InventoryService;
import com.corporate.vending.application.service.OrderMapper;
import com.corporate.vending.application.service.OrderServiceImpl;
import com.corporate.vending.application.service.PaymentService;
import com.corporate.vending.application.service.RefundHandler;
import com.corporate.vending.application.statemachine.OrderStateMachineService;
import com.corporate.vending.domain.MachineStatus;
import com.corporate.vending.domain.PaymentMethod;
import com.corporate.vending.domain.PaymentStatus;
import com.corporate.vending.domain.SlotStatus;
import com.corporate.vending.domain.VendingEvent;
import com.corporate.vending.domain.VendingState;
import com.corporate.vending.domain.exception.InvalidStateException;
import com.corporate.vending.domain.exception.SlotUnavailableException;
import com.corporate.vending.domain.model.Machine;
import com.corporate.vending.domain.model.OrderEntity;
import com.corporate.vending.domain.model.OrderStateAudit;
import com.corporate.vending.domain.model.PaymentEntity;
import com.corporate.vending.domain.model.Product;
import com.corporate.vending.domain.model.Slot;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Disabled
class OrderServiceTest {

    @Mock OrderRepository orders;
    @Mock MachineRepository machines;
    @Mock SlotRepository slots;
    @Mock PaymentRepository payments;
    @Mock RfidBadgeRepository badges;
    @Mock OrderStateAuditRepository audits;
    @Mock OrderStateMachineService stateMachines;
    @Mock PaymentService paymentService;
    @Mock InventoryService inventory;
    @Mock RefundHandler refundHandler;
    @Mock OrderMapper mapper;

    @InjectMocks OrderServiceImpl service;

    private final UUID machineId = UUID.randomUUID();
    private final UUID slotId = UUID.randomUUID();
    private Machine machine;
    private Slot slot;
    private Product product;

    @BeforeEach
    void setUp() {
        machine = new Machine();
        machine.setId(machineId);
        machine.setStatus(MachineStatus.ONLINE);
        product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Cold Coffee");
        product.setPricePaise(6000);
        slot = new Slot();
        slot.setId(slotId);
        slot.setMachine(machine);
        slot.setSlotNo("A1");
        slot.setProduct(product);
        slot.setStatus(SlotStatus.ACTIVE);
        slot.setQuantity(5);
    }

    private OrderEntity order(VendingState state) {
        OrderEntity o = new OrderEntity();
        o.setId(UUID.randomUUID());
        o.setMachine(machine);
        o.setSlot(slot);
        o.setProduct(product);
        o.setAmountPaise(6000);
        o.setState(state);
        return o;
    }

    private OrderResponse response(OrderEntity o) {
        return new OrderResponse(o.getId(), machineId, "A1", "Cold Coffee", 6000, o.getState());
    }

    @Test
    void createOrder_sameIdempotencyKey_returnsExistingOrderWithoutReserving() {
        OrderEntity existing = order(VendingState.PAYMENT_PENDING);
        when(orders.findByIdempotencyKey("k1")).thenReturn(Optional.of(existing));
        when(mapper.toResponse(existing)).thenReturn(response(existing));

        OrderResponse result = service.createOrder("k1", new CreateOrderRequest(machineId, "A1"));

        assertThat(result.id()).isEqualTo(existing.getId());
        verifyNoInteractions(stateMachines, inventory);
    }

    @Test
    void createOrder_happyPath_endsInPaymentPending() {
        when(orders.findByIdempotencyKey("k2")).thenReturn(Optional.empty());
        when(machines.findById(machineId)).thenReturn(Optional.of(machine));
        when(slots.findByMachineIdAndSlotNo(machineId, "A1")).thenReturn(Optional.of(slot));
        when(orders.saveAndFlush(any(OrderEntity.class))).thenAnswer(inv -> {
            OrderEntity o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });
        when(stateMachines.fire(any(), eq(machineId), eq("A1"), eq(VendingState.IDLE), eq(VendingEvent.SELECT_ITEM)))
                .thenReturn(VendingState.ITEM_SELECTED);
        when(stateMachines.fire(any(), eq(machineId), eq("A1"), eq(VendingState.ITEM_SELECTED),
                eq(VendingEvent.INITIATE_PAYMENT))).thenReturn(VendingState.PAYMENT_PENDING);
        when(mapper.toResponse(any(OrderEntity.class))).thenAnswer(inv -> response(inv.getArgument(0)));

        OrderResponse result = service.createOrder("k2", new CreateOrderRequest(machineId, "A1"));

        assertThat(result.state()).isEqualTo(VendingState.PAYMENT_PENDING);
        assertThat(result.amountPaise()).isEqualTo(6000);
        verify(audits, org.mockito.Mockito.times(2)).save(any(OrderStateAudit.class));
    }

    @Test
    void createOrder_guardRejectsSelection_throwsSlotUnavailable() {
        when(orders.findByIdempotencyKey("k3")).thenReturn(Optional.empty());
        when(machines.findById(machineId)).thenReturn(Optional.of(machine));
        when(slots.findByMachineIdAndSlotNo(machineId, "A1")).thenReturn(Optional.of(slot));
        when(orders.saveAndFlush(any(OrderEntity.class))).thenAnswer(inv -> {
            OrderEntity o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });
        when(stateMachines.fire(any(), any(), any(), eq(VendingState.IDLE), eq(VendingEvent.SELECT_ITEM)))
                .thenReturn(VendingState.IDLE);

        assertThatThrownBy(() -> service.createOrder("k3", new CreateOrderRequest(machineId, "A1")))
                .isInstanceOf(SlotUnavailableException.class);
    }

    @Test
    void createOrder_faultySlot_isRejectedBeforeAnyOrderIsSaved() {
        slot.setStatus(SlotStatus.FAULTY);
        when(orders.findByIdempotencyKey("k4")).thenReturn(Optional.empty());
        when(machines.findById(machineId)).thenReturn(Optional.of(machine));
        when(slots.findByMachineIdAndSlotNo(machineId, "A1")).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> service.createOrder("k4", new CreateOrderRequest(machineId, "A1")))
                .isInstanceOf(SlotUnavailableException.class);
        verify(orders, org.mockito.Mockito.never()).saveAndFlush(any());
    }

    @Test
    void pay_rfidSuccess_movesOrderToDispensing() {
        OrderEntity o = order(VendingState.PAYMENT_PENDING);
        when(orders.findById(o.getId())).thenReturn(Optional.of(o));
        when(payments.existsByOrderIdAndStatusIn(eq(o.getId()), any())).thenReturn(false);
        when(badges.findByBadgeHash("hash")).thenReturn(Optional.empty());
        when(paymentService.pay(any())).thenReturn(PaymentResult.success("tx-1"));
        when(stateMachines.fire(eq(o.getId()), eq(machineId), eq("A1"), eq(VendingState.PAYMENT_PENDING),
                eq(VendingEvent.PAYMENT_SUCCESS))).thenReturn(VendingState.DISPENSING);

        PaymentResponse response = service.pay(o.getId(), new PayRequest(PaymentMethod.RFID, "hash", null, null));

        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.orderState()).isEqualTo(VendingState.DISPENSING);
        ArgumentCaptor<PaymentEntity> captor = ArgumentCaptor.forClass(PaymentEntity.class);
        verify(payments).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(captor.getValue().getGatewayTxnId()).isEqualTo("tx-1");
    }

    @Test
    void pay_upi_returnsQrAndKeepsOrderPending() {
        OrderEntity o = order(VendingState.PAYMENT_PENDING);
        when(orders.findById(o.getId())).thenReturn(Optional.of(o));
        when(payments.existsByOrderIdAndStatusIn(eq(o.getId()), any())).thenReturn(false);
        when(paymentService.pay(any())).thenReturn(PaymentResult.pending("upi-1", "upi://pay"));

        PaymentResponse response = service.pay(o.getId(), new PayRequest(PaymentMethod.UPI, null, null, null));

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.qrPayload()).isEqualTo("upi://pay");
        assertThat(response.orderState()).isEqualTo(VendingState.PAYMENT_PENDING);
        verifyNoInteractions(stateMachines);
    }

    @Test
    void pay_whenOrderNotAwaitingPayment_throwsInvalidState() {
        OrderEntity o = order(VendingState.DISPENSING);
        when(orders.findById(o.getId())).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> service.pay(o.getId(), new PayRequest(PaymentMethod.CARD, null, "tok", null)))
                .isInstanceOf(InvalidStateException.class);
        verifyNoInteractions(paymentService);
    }

    @Test
    void onDropResult_duplicateDelivery_isIgnored() {
        OrderEntity o = order(VendingState.DISPENSED);
        when(orders.findById(o.getId())).thenReturn(Optional.of(o));

        service.onDropResult(o.getId(), true);

        verifyNoInteractions(stateMachines, inventory);
    }

    @Test
    void onDropResult_success_confirmsDropAndDecrementsStock() {
        OrderEntity o = order(VendingState.DISPENSING);
        when(orders.findById(o.getId())).thenReturn(Optional.of(o));
        when(stateMachines.fire(eq(o.getId()), eq(machineId), eq("A1"), eq(VendingState.DISPENSING),
                eq(VendingEvent.DROP_CONFIRMED))).thenReturn(VendingState.DISPENSED);

        service.onDropResult(o.getId(), true);

        assertThat(o.getState()).isEqualTo(VendingState.DISPENSED);
        verify(slots).decrementQuantity(slotId);
        verify(inventory).complete(machineId, "A1", o.getId());
    }

    @Test
    void onDropResult_failure_refundsAndIsolatesFaultySlot() {
        OrderEntity o = order(VendingState.DISPENSING);
        when(orders.findById(o.getId())).thenReturn(Optional.of(o));
        when(inventory.recordFault(machineId, "A1")).thenReturn(true);
        when(stateMachines.fire(eq(o.getId()), eq(machineId), eq("A1"), eq(VendingState.DISPENSING),
                eq(VendingEvent.DROP_FAILED))).thenReturn(VendingState.REFUNDING);
        when(stateMachines.fire(eq(o.getId()), eq(machineId), eq("A1"), eq(VendingState.REFUNDING),
                eq(VendingEvent.REFUND_DONE))).thenReturn(VendingState.REFUNDED);
        when(inventory.hasAnyActiveSlot(machineId)).thenReturn(false);

        service.onDropResult(o.getId(), false);

        assertThat(o.getState()).isEqualTo(VendingState.REFUNDED);
        verify(slots).updateStatus(slotId, SlotStatus.FAULTY);
        verify(machines).updateStatus(machineId, MachineStatus.OUT_OF_SERVICE);
    }

    @Test
    void expireOrder_cancelsUnpaidOrder() {
        OrderEntity o = order(VendingState.PAYMENT_PENDING);
        when(orders.findById(o.getId())).thenReturn(Optional.of(o));
        when(stateMachines.fire(eq(o.getId()), eq(machineId), eq("A1"), eq(VendingState.PAYMENT_PENDING),
                eq(VendingEvent.TIMEOUT))).thenReturn(VendingState.IDLE);

        service.expireOrder(o.getId());

        assertThat(o.getState()).isEqualTo(VendingState.IDLE);
    }
}
