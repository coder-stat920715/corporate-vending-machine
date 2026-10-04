package com.corporate.vending.adapter.out.mqtt;

import com.corporate.vending.application.port.out.MqttPublisherPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class MqttPublisherAdapter implements MqttPublisherPort {

    private final MessageChannel outbound;

    public MqttPublisherAdapter(@Qualifier("mqttOutboundChannel") MessageChannel outbound) {
        this.outbound = outbound;
    }

    @Override
    public void publish(String topic, String payload, int qos) {
        outbound.send(MessageBuilder.withPayload(payload)
                .setHeader(MqttHeaders.TOPIC, topic)
                .setHeader(MqttHeaders.QOS, qos)
                .build());
    }
}
