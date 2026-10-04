package com.corporate.vending.application.dto;

import com.corporate.vending.domain.SlotStatus;
import jakarta.validation.constraints.NotNull;

public record SlotStatusRequest(@NotNull SlotStatus status) { }
