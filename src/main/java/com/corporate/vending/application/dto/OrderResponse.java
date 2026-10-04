package com.corporate.vending.application.dto;

import com.corporate.vending.domain.VendingState;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID machineId,
        String slotNo,
        String productName,
        long amountPaise,
        VendingState state) { }
