package com.corporate.vending.application.payment;

import com.corporate.vending.application.port.out.WalletPort;
import com.corporate.vending.domain.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RfidPaymentStrategy implements PaymentStrategy {

    private final WalletPort walletPort;

    @Override
    public PaymentMethod method() {
        return PaymentMethod.RFID;
    }

    @Override
    public PaymentResult pay(PaymentRequest r) {
        if (r.badgeHash() == null || r.badgeHash().isBlank()) {
            return PaymentResult.failed("Badge not presented");
        }
        return walletPort.debit(r.badgeHash(), r.amountPaise(), r.orderId())
                .map(PaymentResult::success)
                .orElseGet(() -> PaymentResult.failed("Insufficient balance or badge blocked"));
    }

    @Override
    public void refund(PaymentRequest r) {
        walletPort.credit(r.badgeHash(), r.amountPaise(), r.orderId());
    }
}
