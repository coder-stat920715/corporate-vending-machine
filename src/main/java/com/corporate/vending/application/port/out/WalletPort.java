package com.corporate.vending.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface WalletPort {

    /** Atomically debits; returns the wallet transaction id, or empty if funds are short or the badge is invalid. */
    Optional<String> debit(String badgeHash, long amountPaise, UUID orderId);

    /** Idempotent per order: a second refund for the same order is a no-op. */
    void credit(String badgeHash, long amountPaise, UUID orderId);
}
