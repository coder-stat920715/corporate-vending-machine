package com.corporate.vending.application.payment;

import com.corporate.vending.domain.PaymentMethod;

public interface PaymentStrategy {
    PaymentMethod method();
    PaymentResult pay(PaymentRequest request);
    void refund(PaymentRequest request);
}
