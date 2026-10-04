package com.corporate.vending.application.statemachine;

import com.corporate.vending.domain.VendingEvent;
import com.corporate.vending.domain.VendingState;
import java.util.EnumSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

@Slf4j
@Configuration
@EnableStateMachineFactory
@RequiredArgsConstructor
public class StateMachineConfig extends EnumStateMachineConfigurerAdapter<VendingState, VendingEvent> {

    private final SlotAvailableGuard slotAvailableGuard;
    private final ReserveStockAction reserveStockAction;
    private final ReleaseStockAction releaseStockAction;
    private final DispatchDispenseAction dispatchDispenseAction;
    private final RefundAction refundAction;

    @Override
    public void configure(StateMachineConfigurationConfigurer<VendingState, VendingEvent> config) throws Exception {
        config.withConfiguration()
              .autoStartup(false)
              .listener(new StateMachineListenerAdapter<VendingState, VendingEvent>() {
                  @Override
                  public void stateChanged(State<VendingState, VendingEvent> from, State<VendingState, VendingEvent> to) {
                      log.debug("State {} -> {}", from == null ? null : from.getId(), to.getId());
                  }
              });
    }

    @Override
    public void configure(StateMachineStateConfigurer<VendingState, VendingEvent> states) throws Exception {
        states.withStates()
              .initial(VendingState.IDLE)
              .states(EnumSet.allOf(VendingState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<VendingState, VendingEvent> t) throws Exception {
        t.withExternal().source(VendingState.IDLE).target(VendingState.ITEM_SELECTED)
                .event(VendingEvent.SELECT_ITEM).guard(slotAvailableGuard).action(reserveStockAction)
         .and().withExternal().source(VendingState.ITEM_SELECTED).target(VendingState.PAYMENT_PENDING)
                .event(VendingEvent.INITIATE_PAYMENT)
         .and().withExternal().source(VendingState.ITEM_SELECTED).target(VendingState.IDLE)
                .event(VendingEvent.TIMEOUT).action(releaseStockAction)
         .and().withExternal().source(VendingState.PAYMENT_PENDING).target(VendingState.DISPENSING)
                .event(VendingEvent.PAYMENT_SUCCESS).action(dispatchDispenseAction)
         .and().withExternal().source(VendingState.PAYMENT_PENDING).target(VendingState.IDLE)
                .event(VendingEvent.PAYMENT_FAILED).action(releaseStockAction)
         .and().withExternal().source(VendingState.PAYMENT_PENDING).target(VendingState.IDLE)
                .event(VendingEvent.TIMEOUT).action(releaseStockAction)
         .and().withExternal().source(VendingState.DISPENSING).target(VendingState.DISPENSED)
                .event(VendingEvent.DROP_CONFIRMED)
         .and().withExternal().source(VendingState.DISPENSING).target(VendingState.REFUNDING)
                .event(VendingEvent.DROP_FAILED).action(refundAction)
         .and().withExternal().source(VendingState.REFUNDING).target(VendingState.REFUNDED)
                .event(VendingEvent.REFUND_DONE)
         .and().withExternal().source(VendingState.DISPENSED).target(VendingState.IDLE)
                .event(VendingEvent.RESET)
         .and().withExternal().source(VendingState.REFUNDED).target(VendingState.IDLE)
                .event(VendingEvent.RESET)
         .and().withExternal().source(VendingState.IDLE).target(VendingState.OUT_OF_SERVICE)
                .event(VendingEvent.MACHINE_FAULT)
         .and().withExternal().source(VendingState.OUT_OF_SERVICE).target(VendingState.IDLE)
                .event(VendingEvent.RESET);
    }
}
