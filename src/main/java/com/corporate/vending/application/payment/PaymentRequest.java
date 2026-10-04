package com.corporate.vending.application.payment;

import com.corporate.vending.domain.PaymentMethod;
import java.util.UUID;

public record PaymentRequest(
        UUID orderId,
        PaymentMethod method,
        long amountPaise,
        String badgeHash,
        String cardToken,
        String payerVpa,
        String gatewayTxnId) { }
