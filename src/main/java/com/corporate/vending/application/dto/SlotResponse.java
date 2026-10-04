package com.corporate.vending.application.dto;

import com.corporate.vending.domain.SlotStatus;

public record SlotResponse(
        String slotNo,
        String productName,
        long pricePaise,
        int quantity,
        SlotStatus status,
        boolean available) { }
