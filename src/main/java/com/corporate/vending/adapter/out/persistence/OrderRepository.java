package com.corporate.vending.adapter.out.persistence;

import com.corporate.vending.domain.VendingState;
import com.corporate.vending.domain.model.OrderEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByIdempotencyKey(String idempotencyKey);

    List<OrderEntity> findByStateAndUpdatedAtBefore(VendingState state, Instant cutoff);
}
