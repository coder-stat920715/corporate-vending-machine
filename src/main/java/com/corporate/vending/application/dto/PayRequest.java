package com.corporate.vending.application.dto;

import com.corporate.vending.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record PayRequest(
        @NotNull PaymentMethod method,
        String badgeHash,
        String cardToken,
        String payerVpa) { }
