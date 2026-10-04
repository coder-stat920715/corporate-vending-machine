package com.corporate.vending.application.hardware;

import java.time.Instant;
import java.util.UUID;

public record DispenseCommand(UUID commandId, UUID orderId, UUID machineId, String slotNo) implements HardwareCommand {

    @Override
    public String topic() {
        return "vm/" + machineId + "/cmd/dispense";
    }

    @Override
    public String payload() {
        return String.format("{\"commandId\":\"%s\",\"orderId\":\"%s\",\"slotNo\":\"%s\",\"ts\":\"%s\"}",
                commandId, orderId, slotNo, Instant.now());
    }
}
