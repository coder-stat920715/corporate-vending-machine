package com.corporate.vending.application.statemachine;

import com.corporate.vending.application.hardware.DispenseCommand;
import com.corporate.vending.application.service.HardwareDispatcherService;
import com.corporate.vending.domain.VendingEvent;
import com.corporate.vending.domain.VendingState;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DispatchDispenseAction implements Action<VendingState, VendingEvent> {

    private final HardwareDispatcherService dispatcher;

    @Override
    public void execute(StateContext<VendingState, VendingEvent> ctx) {
        var es = ctx.getExtendedState();
        dispatcher.dispatch(new DispenseCommand(
                UUID.randomUUID(),
                es.get("orderId", UUID.class),
                es.get("machineId", UUID.class),
                es.get("slotNo", String.class)));
    }
}
