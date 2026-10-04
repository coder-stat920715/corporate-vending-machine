package com.corporate.vending.adapter.in.rest;

import com.corporate.vending.application.dto.CreateOrderRequest;
import com.corporate.vending.application.dto.OrderResponse;
import com.corporate.vending.application.dto.PayRequest;
import com.corporate.vending.application.dto.PaymentResponse;
import com.corporate.vending.application.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Select an item. Replays with the same Idempotency-Key return the same order.")
    public OrderResponse create(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                @Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(idempotencyKey, request);
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable UUID orderId) {
        return orderService.getOrder(orderId);
    }

    @PostMapping("/{orderId}/pay")
    @Operation(summary = "Pay with RFID badge, UPI (returns QR) or card token.")
    public PaymentResponse pay(@PathVariable UUID orderId, @Valid @RequestBody PayRequest request) {
        return orderService.pay(orderId, request);
    }
}
