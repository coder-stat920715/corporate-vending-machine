package com.corporate.vending.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface CardGatewayPort {

    /** Returns the gateway transaction id if approved, empty if declined. */
    Optional<String> charge(String cardToken, long amountPaise, UUID orderId);

    void refund(String txnId, long amountPaise);
}
