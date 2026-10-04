package com.corporate.vending.adapter.in.mqtt;

import com.corporate.vending.application.service.OrderService;
import com.corporate.vending.domain.event.DropEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Observer: drives the order state machine from the IR sensor result. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DropEventListener {

    private final OrderService orderService;

    @EventListener
    public void on(DropEvent event) {
        try {
            orderService.onDropResult(event.orderId(), event.success());
        } catch (RuntimeException e) {
            log.error("Failed to process drop event for order {}", event.orderId(), e);
        }
    }
}
