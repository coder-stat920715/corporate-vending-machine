package com.corporate.vending.adapter.out.persistence;

import com.corporate.vending.domain.model.RfidBadge;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RfidBadgeRepository extends JpaRepository<RfidBadge, UUID> {

    Optional<RfidBadge> findByBadgeHash(String badgeHash);

    Optional<RfidBadge> findFirstByEmployeeId(UUID employeeId);
}
