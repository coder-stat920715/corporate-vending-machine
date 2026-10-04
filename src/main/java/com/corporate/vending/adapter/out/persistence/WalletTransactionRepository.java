package com.corporate.vending.adapter.out.persistence;

import com.corporate.vending.domain.WalletTxType;
import com.corporate.vending.domain.model.WalletTransaction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    Optional<WalletTransaction> findByOrderIdAndType(UUID orderId, WalletTxType type);
}
