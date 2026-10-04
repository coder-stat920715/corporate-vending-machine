package com.corporate.vending.adapter.out.persistence;

import com.corporate.vending.domain.PaymentStatus;
import com.corporate.vending.domain.model.PaymentEntity;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByGatewayTxnId(String gatewayTxnId);

    Optional<PaymentEntity> findFirstByOrderIdAndStatus(UUID orderId, PaymentStatus status);

    boolean existsByOrderIdAndStatusIn(UUID orderId, Collection<PaymentStatus> statuses);
}
