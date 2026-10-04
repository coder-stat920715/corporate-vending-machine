package com.corporate.vending.application.service;

import com.corporate.vending.adapter.out.persistence.SlotRepository;
import com.corporate.vending.domain.SlotStatus;
import com.corporate.vending.domain.model.Slot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Warms the Redis stock counters and available-slot sets from PostgreSQL at startup (never overwrites live counters). */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockCacheLoader implements ApplicationRunner {

    private final SlotRepository slots;
    private final StringRedisTemplate redis;

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        int count = 0;
        for (Slot slot : slots.findAll()) {
            var machineId = slot.getMachine().getId();
            redis.opsForValue().setIfAbsent(RedisKeys.stock(machineId, slot.getSlotNo()),
                    String.valueOf(slot.getQuantity()));
            if (slot.getStatus() == SlotStatus.ACTIVE && slot.getProduct() != null) {
                redis.opsForSet().add(RedisKeys.available(machineId), slot.getSlotNo());
            } else {
                redis.opsForSet().remove(RedisKeys.available(machineId), slot.getSlotNo());
            }
            count++;
        }
        log.info("Stock cache warmed for {} slots", count);
    }
}
