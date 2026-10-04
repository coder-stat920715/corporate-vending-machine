package com.corporate.vending.application.dto;

import com.corporate.vending.domain.VendingState;

public record PaymentResponse(String status, VendingState orderState, String qrPayload, String message) { }
