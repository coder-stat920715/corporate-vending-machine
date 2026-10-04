package com.corporate.vending.application.service;

import com.corporate.vending.adapter.out.persistence.PaymentRepository;
import com.corporate.vending.adapter.out.persistence.RfidBadgeRepository;
import com.corporate.vending.application.payment.PaymentRequest;
import com.corporate.vending.domain.PaymentMethod;
import com.corporate.vending.domain.PaymentStatus;
import com.corporate.vending.domain.model.PaymentEntity;
import com.corporate.vending.domain.model.RfidBadge;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Refunds the successful payment of an order. Idempotent: only SUCCESS payments are refunded. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundHandler {

    private final PaymentRepository payments;
    private final RfidBadgeRepository badges;
    private final PaymentService paymentService;

    @Transactional
    public void refund(UUID orderId) {
        PaymentEntity payment = payments.findFirstByOrderIdAndStatus(orderId, PaymentStatus.SUCCESS).orElse(null);
        if (payment == null) {
            log.info("No successful payment to refund for order {}", orderId);
            return;
        }
        String badgeHash = null;
        if (payment.getMethod() == PaymentMethod.RFID && payment.getOrder().getEmployee() != null) {
            badgeHash = badges.findFirstByEmployeeId(payment.getOrder().getEmployee().getId())
                    .map(RfidBadge::getBadgeHash).orElse(null);
        }
        paymentService.refund(new PaymentRequest(orderId, payment.getMethod(), payment.getAmountPaise(),
                badgeHash, null, null, payment.getGatewayTxnId()));
        payment.setStatus(PaymentStatus.REFUNDED);
        payments.save(payment);
        log.info("Refunded order {} via {}", orderId, payment.getMethod());
    }
}
