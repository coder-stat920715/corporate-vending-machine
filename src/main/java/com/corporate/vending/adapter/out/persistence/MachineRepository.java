package com.corporate.vending.adapter.out.persistence;

import com.corporate.vending.domain.MachineStatus;
import com.corporate.vending.domain.model.Machine;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MachineRepository extends JpaRepository<Machine, UUID> {

    @Modifying
    @Query("update Machine m set m.status = :status where m.id = :id")
    int updateStatus(@Param("id") UUID id, @Param("status") MachineStatus status);
}
