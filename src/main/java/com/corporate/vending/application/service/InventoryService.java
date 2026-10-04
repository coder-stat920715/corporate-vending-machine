package com.corporate.vending.application.service;

import com.corporate.vending.domain.exception.SlotUnavailableException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Real-time stock and slot reservation backed by Redis.
 * The Redisson RLock guards only the short reserve section (RLock is thread-bound).
 * The "busy" marker, owned by the order id, covers the whole payment + dispense window.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    static final int FAULT_THRESHOLD = 3;
    /** Must exceed payment timeout + dispense timeout so a stale order can still release its stock. */
    static final Duration BUSY_TTL = Duration.ofMinutes(5);

    private final RedissonClient redisson;
    private final StringRedisTemplate redis;

    public boolean isAvailable(UUID machineId, String slotNo) {
        String stock = redis.opsForValue().get(RedisKeys.stock(machineId, slotNo));
        boolean active = Boolean.TRUE.equals(redis.opsForSet().isMember(RedisKeys.available(machineId), slotNo));
        boolean busy = redisson.getBucket(RedisKeys.busy(machineId, slotNo)).isExists();
        return active && !busy && stock != null && Integer.parseInt(stock) > 0;
    }

    public void reserve(UUID machineId, String slotNo, UUID orderId) {
        RLock lock = redisson.getLock(RedisKeys.lock(machineId, slotNo));
        boolean locked = false;
        try {
            locked = lock.tryLock(300, 5000, TimeUnit.MILLISECONDS);
            if (!locked) {
                throw new SlotUnavailableException("Slot is busy: " + slotNo);
            }
            RBucket<String> busy = redisson.getBucket(RedisKeys.busy(machineId, slotNo));
            if (!busy.trySet(orderId.toString(), BUSY_TTL.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new SlotUnavailableException("Slot is reserved by another order: " + slotNo);
            }
            Long remaining = redis.opsForValue().decrement(RedisKeys.stock(machineId, slotNo));
            if (remaining == null || remaining < 0) {
                redis.opsForValue().increment(RedisKeys.stock(machineId, slotNo));
                busy.delete();
                throw new SlotUnavailableException("Sold out: " + slotNo);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SlotUnavailableException("Interrupted while reserving " + slotNo);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /** Releases a reservation owned by this order (payment failed, timeout, or refund). Idempotent per order. */
    public void release(UUID machineId, String slotNo, UUID orderId) {
        RBucket<String> busy = redisson.getBucket(RedisKeys.busy(machineId, slotNo));
        if (orderId.toString().equals(busy.get())) {
            busy.delete();
            redis.opsForValue().increment(RedisKeys.stock(machineId, slotNo));
        }
    }

    /** After a confirmed drop: stock stays decremented, the busy marker and fault counter are cleared. */
    public void complete(UUID machineId, String slotNo, UUID orderId) {
        RBucket<String> busy = redisson.getBucket(RedisKeys.busy(machineId, slotNo));
        if (orderId.toString().equals(busy.get())) {
            busy.delete();
        }
        redis.delete(RedisKeys.fault(machineId, slotNo));
    }

    /** Fault isolation: after N consecutive faults the slot leaves the available set; the machine stays up. */
    public boolean recordFault(UUID machineId, String slotNo) {
        Long faults = redis.opsForValue().increment(RedisKeys.fault(machineId, slotNo));
        if (faults != null && faults >= FAULT_THRESHOLD) {
            redis.opsForSet().remove(RedisKeys.available(machineId), slotNo);
            log.warn("Slot {} on machine {} marked FAULTY after {} consecutive faults", slotNo, machineId, faults);
            return true;
        }
        return false;
    }

    public boolean hasAnyActiveSlot(UUID machineId) {
        Long n = redis.opsForSet().size(RedisKeys.available(machineId));
        return n != null && n > 0;
    }
}
