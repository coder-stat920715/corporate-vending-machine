package com.corporate.vending.application.dto;

import jakarta.validation.constraints.Min;

public record RestockRequest(@Min(0) int quantity) { }
