package com.corporate.vending.application.payment;

import com.corporate.vending.application.port.out.UpiGatewayPort;
import com.corporate.vending.domain.PaymentMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** UPI is asynchronous: pay() returns PENDING with a QR; the PSP webhook later completes the order. */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpiPaymentStrategy implements PaymentStrategy {

    private final UpiGatewayPort gateway;

    @Override
    public PaymentMethod method() {
        return PaymentMethod.UPI;
    }

    @Override
    public PaymentResult pay(PaymentRequest r) {
        try {
            var qr = gateway.createDynamicQr(r.orderId(), r.amountPaise());
            return PaymentResult.pending(qr.txnId(), qr.qrPayload());
        } catch (RuntimeException e) {
            log.error("UPI QR creation failed for order {}", r.orderId(), e);
            return PaymentResult.failed("UPI gateway unavailable");
        }
    }

    @Override
    public void refund(PaymentRequest r) {
        gateway.refund(r.gatewayTxnId(), r.amountPaise());
    }
}
