package com.corporate.vending.application.service;

import com.corporate.vending.adapter.out.persistence.OrderRepository;
import com.corporate.vending.domain.VendingState;
import com.corporate.vending.domain.model.OrderEntity;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sweeps stuck orders: a dispense with no sensor reply is treated as a failed drop (refund),
 * and an unpaid order past the payment window is cancelled. A Redis lock keeps multiple instances from racing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private final OrderRepository orders;
    private final OrderService orderService;
    private final RedissonClient redisson;

    @Value("${vending.order.payment-timeout-seconds:120}")
    private long paymentTimeoutSeconds;

    @Value("${vending.order.dispense-timeout-seconds:20}")
    private long dispenseTimeoutSeconds;

    @Scheduled(fixedDelayString = "${vending.order.watchdog-interval-ms:5000}")
    public void sweep() {
        RLock lock = redisson.getLock(RedisKeys.watchdogLock());
        boolean locked = false;
        try {
            locked = lock.tryLock(0, 30, TimeUnit.SECONDS);
            if (!locked) {
                return;
            }
            Instant now = Instant.now();
            List<OrderEntity> stuckDispenses =
                    orders.findByStateAndUpdatedAtBefore(VendingState.DISPENSING, now.minusSeconds(dispenseTimeoutSeconds));
            for (OrderEntity o : stuckDispenses) {
                try {
                    log.warn("Dispense timeout for order {}", o.getId());
                    orderService.onDropResult(o.getId(), false);
                } catch (RuntimeException e) {
                    log.error("Failed to handle dispense timeout for order {}", o.getId(), e);
                }
            }
            List<OrderEntity> unpaid =
                    orders.findByStateAndUpdatedAtBefore(VendingState.PAYMENT_PENDING, now.minusSeconds(paymentTimeoutSeconds));
            for (OrderEntity o : unpaid) {
                try {
                    orderService.expireOrder(o.getId());
                } catch (RuntimeException e) {
                    log.error("Failed to expire order {}", o.getId(), e);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
