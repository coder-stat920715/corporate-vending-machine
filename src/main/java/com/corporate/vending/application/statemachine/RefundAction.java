package com.corporate.vending.application.statemachine;

import com.corporate.vending.application.service.InventoryService;
import com.corporate.vending.application.service.RefundHandler;
import com.corporate.vending.domain.VendingEvent;
import com.corporate.vending.domain.VendingState;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefundAction implements Action<VendingState, VendingEvent> {

    private final RefundHandler refundHandler;
    private final InventoryService inventoryService;

    @Override
    public void execute(StateContext<VendingState, VendingEvent> ctx) {
        var es = ctx.getExtendedState();
        UUID orderId = es.get("orderId", UUID.class);
        refundHandler.refund(orderId);
        inventoryService.release(es.get("machineId", UUID.class), es.get("slotNo", String.class), orderId);
    }
}
