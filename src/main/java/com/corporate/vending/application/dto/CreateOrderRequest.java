package com.corporate.vending.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull UUID machineId,
        @NotBlank @Size(max = 6) String slotNo) { }
