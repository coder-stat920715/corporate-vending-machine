package com.corporate.vending.application.payment;

import com.corporate.vending.application.port.out.CardGatewayPort;
import com.corporate.vending.domain.PaymentMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardPaymentStrategy implements PaymentStrategy {

    private final CardGatewayPort gateway;

    @Override
    public PaymentMethod method() {
        return PaymentMethod.CARD;
    }

    @Override
    public PaymentResult pay(PaymentRequest r) {
        if (r.cardToken() == null || r.cardToken().isBlank()) {
            return PaymentResult.failed("Card token missing");
        }
        try {
            return gateway.charge(r.cardToken(), r.amountPaise(), r.orderId())
                    .map(PaymentResult::success)
                    .orElseGet(() -> PaymentResult.failed("Card declined"));
        } catch (RuntimeException e) {
            log.error("Card charge failed for order {}", r.orderId(), e);
            return PaymentResult.failed("Card gateway unavailable");
        }
    }

    @Override
    public void refund(PaymentRequest r) {
        gateway.refund(r.gatewayTxnId(), r.amountPaise());
    }
}
