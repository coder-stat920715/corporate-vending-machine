package com.corporate.vending.application.service;

import com.corporate.vending.application.dto.CreateOrderRequest;
import com.corporate.vending.application.dto.OrderResponse;
import com.corporate.vending.application.dto.PayRequest;
import com.corporate.vending.application.dto.PaymentResponse;
import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(String idempotencyKey, CreateOrderRequest request);

    OrderResponse getOrder(UUID orderId);

    PaymentResponse pay(UUID orderId, PayRequest request);

    /** Called by the PSP webhook once the UPI payment outcome is known. Duplicate deliveries are ignored. */
    void onUpiPaymentResult(UUID orderId, String gatewayTxnId, boolean success);

    /** Called by the IR sensor observer (or the watchdog on timeout). Duplicate deliveries are ignored. */
    void onDropResult(UUID orderId, boolean success);

    /** Cancels an order whose payment window elapsed. */
    void expireOrder(UUID orderId);
}
