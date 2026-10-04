package com.corporate.vending.application.statemachine;

import com.corporate.vending.domain.VendingEvent;
import com.corporate.vending.domain.VendingState;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OrderStateMachineService {

    private final StateMachineFactory<VendingState, VendingEvent> factory;

    /**
     * Rehydrates a machine at the order's persisted state, sends one event, and returns the resulting state.
     * If the event is not allowed (or a guard rejects it) the returned state equals {@code current}.
     * Exceptions thrown by actions are re-thrown so the surrounding DB transaction rolls back.
     */
    public VendingState fire(UUID orderId, UUID machineId, String slotNo, VendingState current, VendingEvent event) {
        StateMachine<VendingState, VendingEvent> sm = factory.getStateMachine(orderId.toString());
        AtomicReference<Exception> error = new AtomicReference<>();
        sm.addStateListener(new StateMachineListenerAdapter<VendingState, VendingEvent>() {
            @Override
            public void stateMachineError(StateMachine<VendingState, VendingEvent> machine, Exception exception) {
                error.set(exception);
            }
        });
        try {
            sm.stopReactively().block();
            sm.getStateMachineAccessor().doWithAllRegions(access ->
                    access.resetStateMachineReactively(new DefaultStateMachineContext<>(current, null, null, null)).block());
            sm.getExtendedState().getVariables().put("orderId", orderId);
            sm.getExtendedState().getVariables().put("machineId", machineId);
            sm.getExtendedState().getVariables().put("slotNo", slotNo);
            sm.startReactively().block();

            sm.sendEvent(Mono.just(MessageBuilder.withPayload(event).build())).blockLast();

            if (error.get() != null) {
                throw unwrap(error.get());
            }
            return sm.getState().getId();
        } finally {
            sm.stopReactively().block();
        }
    }

    private RuntimeException unwrap(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof RuntimeException re && c.getClass().getName().startsWith("com.corporate.vending")) {
                return re;
            }
        }
        return t instanceof RuntimeException re ? re : new IllegalStateException(t);
    }
}
