package com.corporate.vending.domain.event;

import java.util.UUID;

/** Published when the IR sensor on a machine reports the outcome of a dispense. */
public record DropEvent(UUID machineId, UUID orderId, Result result) {

    public enum Result { OK, TIMEOUT, JAM }

    public boolean success() {
        return result == Result.OK;
    }
}
