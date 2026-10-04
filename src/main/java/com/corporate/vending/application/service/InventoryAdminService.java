package com.corporate.vending.application.service;

import com.corporate.vending.adapter.out.persistence.SlotRepository;
import com.corporate.vending.application.dto.SlotResponse;
import com.corporate.vending.domain.SlotStatus;
import com.corporate.vending.domain.exception.ResourceNotFoundException;
import com.corporate.vending.domain.model.Slot;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryAdminService {

    private final SlotRepository slots;
    private final StringRedisTemplate redis;
    private final InventoryService inventory;

    @Transactional(readOnly = true)
    public List<SlotResponse> listSlots(UUID machineId) {
        return slots.findByMachineIdOrderBySlotNo(machineId).stream().map(s -> toResponse(machineId, s)).toList();
    }

    @Transactional
    public SlotResponse restock(UUID machineId, String slotNo, int quantity) {
        Slot slot = find(machineId, slotNo);
        if (quantity > slot.getCapacity()) {
            throw new IllegalArgumentException("Quantity " + quantity + " exceeds slot capacity " + slot.getCapacity());
        }
        slot.setQuantity(quantity);
        slots.save(slot);
        redis.opsForValue().set(RedisKeys.stock(machineId, slotNo), String.valueOf(quantity));
        syncAvailability(machineId, slot);
        return toResponse(machineId, slot);
    }

    @Transactional
    public SlotResponse setStatus(UUID machineId, String slotNo, SlotStatus status) {
        Slot slot = find(machineId, slotNo);
        slot.setStatus(status);
        if (status == SlotStatus.ACTIVE) {
            slot.setFaultCount(0);
            redis.delete(RedisKeys.fault(machineId, slotNo));
        }
        slots.save(slot);
        syncAvailability(machineId, slot);
        return toResponse(machineId, slot);
    }

    private void syncAvailability(UUID machineId, Slot slot) {
        if (slot.getStatus() == SlotStatus.ACTIVE && slot.getProduct() != null) {
            redis.opsForSet().add(RedisKeys.available(machineId), slot.getSlotNo());
        } else {
            redis.opsForSet().remove(RedisKeys.available(machineId), slot.getSlotNo());
        }
    }

    private Slot find(UUID machineId, String slotNo) {
        return slots.findByMachineIdAndSlotNo(machineId, slotNo)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + slotNo));
    }

    private SlotResponse toResponse(UUID machineId, Slot s) {
        return new SlotResponse(
                s.getSlotNo(),
                s.getProduct() == null ? null : s.getProduct().getName(),
                s.getProduct() == null ? 0 : s.getProduct().getPricePaise(),
                s.getQuantity(),
                s.getStatus(),
                inventory.isAvailable(machineId, s.getSlotNo()));
    }
}
