package com.corporate.vending.adapter.out.persistence;

import com.corporate.vending.domain.model.OrderStateAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStateAuditRepository extends JpaRepository<OrderStateAudit, Long> { }
