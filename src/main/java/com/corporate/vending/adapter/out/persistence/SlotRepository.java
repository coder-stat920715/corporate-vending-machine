package com.corporate.vending.adapter.out.persistence;

import com.corporate.vending.domain.SlotStatus;
import com.corporate.vending.domain.model.Slot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SlotRepository extends JpaRepository<Slot, UUID> {

    Optional<Slot> findByMachineIdAndSlotNo(UUID machineId, String slotNo);

    List<Slot> findByMachineIdOrderBySlotNo(UUID machineId);

    @Modifying
    @Query("update Slot s set s.quantity = s.quantity - 1 where s.id = :id and s.quantity > 0")
    int decrementQuantity(@Param("id") UUID id);

    @Modifying
    @Query("update Slot s set s.faultCount = s.faultCount + 1 where s.id = :id")
    int incrementFaultCount(@Param("id") UUID id);

    @Modifying
    @Query("update Slot s set s.faultCount = 0 where s.id = :id")
    int resetFaultCount(@Param("id") UUID id);

    @Modifying
    @Query("update Slot s set s.status = :status where s.id = :id")
    int updateStatus(@Param("id") UUID id, @Param("status") SlotStatus status);
}
