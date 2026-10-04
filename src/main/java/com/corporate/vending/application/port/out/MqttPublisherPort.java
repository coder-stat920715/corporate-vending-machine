package com.corporate.vending.application.port.out;

public interface MqttPublisherPort {
    void publish(String topic, String payload, int qos);
}
