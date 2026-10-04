package com.corporate.vending.application.service;

import com.corporate.vending.application.payment.PaymentRequest;
import com.corporate.vending.application.payment.PaymentResult;
import com.corporate.vending.application.payment.PaymentStrategy;
import com.corporate.vending.domain.PaymentMethod;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final Map<PaymentMethod, PaymentStrategy> strategies = new EnumMap<>(PaymentMethod.class);

    public PaymentService(List<PaymentStrategy> all) {
        all.forEach(s -> strategies.put(s.method(), s));
    }

    public PaymentResult pay(PaymentRequest request) {
        return resolve(request.method()).pay(request);
    }

    public void refund(PaymentRequest request) {
        resolve(request.method()).refund(request);
    }

    private PaymentStrategy resolve(PaymentMethod method) {
        PaymentStrategy strategy = strategies.get(method);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported payment method: " + method);
        }
        return strategy;
    }
}
