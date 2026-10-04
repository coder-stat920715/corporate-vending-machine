package com.corporate.vending.application.statemachine;

import com.corporate.vending.application.service.InventoryService;
import com.corporate.vending.domain.VendingEvent;
import com.corporate.vending.domain.VendingState;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SlotAvailableGuard implements Guard<VendingState, VendingEvent> {

    private final InventoryService inventoryService;

    @Override
    public boolean evaluate(StateContext<VendingState, VendingEvent> ctx) {
        UUID machineId = ctx.getExtendedState().get("machineId", UUID.class);
        String slotNo = ctx.getExtendedState().get("slotNo", String.class);
        return machineId != null && slotNo != null && inventoryService.isAvailable(machineId, slotNo);
    }
}
