package com.corporate.vending.adapter.in.ws;

import com.corporate.vending.domain.event.DropEvent;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Observer: pushes the dispense result to the touchscreen / mobile client. */
@Component
@RequiredArgsConstructor
public class MachineStatusBroadcaster {

    private final SimpMessagingTemplate template;

    @EventListener
    public void on(DropEvent event) {
        template.convertAndSend("/topic/machines/" + event.machineId() + "/orders/" + event.orderId(),
                Map.of("orderId", event.orderId().toString(), "result", event.result().name()));
    }
}
