package com.corporate.vending.application.service;

import com.corporate.vending.application.hardware.HardwareCommand;
import com.corporate.vending.application.port.out.MqttPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Executes hardware commands. When called inside a DB transaction, the MQTT publish is deferred until
 * after commit so the edge can never answer before the order row shows DISPENSING, and nothing is sent on rollback.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HardwareDispatcherService {

    private final MqttPublisherPort publisher;

    public void dispatch(HardwareCommand command) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(command);
                }
            });
        } else {
            send(command);
        }
    }

    private void send(HardwareCommand command) {
        log.info("Dispatching {} to {}", command.getClass().getSimpleName(), command.topic());
        publisher.publish(command.topic(), command.payload(), 1);
    }
}
