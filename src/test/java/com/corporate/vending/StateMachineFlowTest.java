package com.corporate.vending;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.corporate.vending.application.statemachine.DispatchDispenseAction;
import com.corporate.vending.application.statemachine.OrderStateMachineService;
import com.corporate.vending.application.statemachine.RefundAction;
import com.corporate.vending.application.statemachine.ReleaseStockAction;
import com.corporate.vending.application.statemachine.ReserveStockAction;
import com.corporate.vending.application.statemachine.SlotAvailableGuard;
import com.corporate.vending.application.statemachine.StateMachineConfig;
import com.corporate.vending.domain.VendingEvent;
import com.corporate.vending.domain.VendingState;
import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {StateMachineConfig.class, OrderStateMachineService.class})
@Disabled
class StateMachineFlowTest {

    @MockBean SlotAvailableGuard guard;
    @MockBean ReserveStockAction reserve;
    @MockBean ReleaseStockAction release;
    @MockBean DispatchDispenseAction dispatch;
    @MockBean RefundAction refund;

    @Autowired OrderStateMachineService service;

    private final UUID orderId = UUID.randomUUID();
    private final UUID machineId = UUID.randomUUID();

    private VendingState fire(VendingState from, VendingEvent event) {
        return service.fire(orderId, machineId, "A1", from, event);
    }

    @Test
    void happyPath_reachesDispensed() {
        when(guard.evaluate(any())).thenReturn(true);

        VendingState s = fire(VendingState.IDLE, VendingEvent.SELECT_ITEM);
        assertThat(s).isEqualTo(VendingState.ITEM_SELECTED);
        s = fire(s, VendingEvent.INITIATE_PAYMENT);
        assertThat(s).isEqualTo(VendingState.PAYMENT_PENDING);
        s = fire(s, VendingEvent.PAYMENT_SUCCESS);
        assertThat(s).isEqualTo(VendingState.DISPENSING);
        s = fire(s, VendingEvent.DROP_CONFIRMED);
        assertThat(s).isEqualTo(VendingState.DISPENSED);

        verify(reserve).execute(any());
        verify(dispatch).execute(any());
    }

    @Test
    void sensorFailure_goesThroughRefunding_toRefunded() {
        VendingState s = fire(VendingState.DISPENSING, VendingEvent.DROP_FAILED);
        assertThat(s).isEqualTo(VendingState.REFUNDING);
        verify(refund).execute(any());

        s = fire(s, VendingEvent.REFUND_DONE);
        assertThat(s).isEqualTo(VendingState.REFUNDED);
    }

    @Test
    void guardRejection_keepsIdle() {
        when(guard.evaluate(any())).thenReturn(false);

        assertThat(fire(VendingState.IDLE, VendingEvent.SELECT_ITEM)).isEqualTo(VendingState.IDLE);
    }

    @Test
    void illegalEvent_isIgnored() {
        assertThat(fire(VendingState.IDLE, VendingEvent.PAYMENT_SUCCESS)).isEqualTo(VendingState.IDLE);
        assertThat(fire(VendingState.DISPENSED, VendingEvent.DROP_FAILED)).isEqualTo(VendingState.DISPENSED);
    }

    @Test
    void paymentTimeout_releasesStock() {
        assertThat(fire(VendingState.PAYMENT_PENDING, VendingEvent.TIMEOUT)).isEqualTo(VendingState.IDLE);
        verify(release).execute(any());
    }
}
