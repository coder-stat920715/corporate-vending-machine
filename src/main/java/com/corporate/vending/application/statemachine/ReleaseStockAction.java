package com.corporate.vending.application.statemachine;

import com.corporate.vending.application.service.InventoryService;
import com.corporate.vending.domain.VendingEvent;
import com.corporate.vending.domain.VendingState;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReleaseStockAction implements Action<VendingState, VendingEvent> {

    private final InventoryService inventoryService;

    @Override
    public void execute(StateContext<VendingState, VendingEvent> ctx) {
        var es = ctx.getExtendedState();
        inventoryService.release(es.get("machineId", UUID.class), es.get("slotNo", String.class),
                es.get("orderId", UUID.class));
    }
}
