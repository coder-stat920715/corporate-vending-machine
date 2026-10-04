package com.corporate.vending.adapter.in.mqtt;

import com.corporate.vending.application.service.RedisKeys;
import com.corporate.vending.domain.event.DropEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttInboundHandler {

    private final ObjectMapper mapper;
    private final ApplicationEventPublisher publisher;
    private final StringRedisTemplate redis;

    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public void handle(Message<String> message) {
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        if (topic == null) {
            return;
        }
        String[] parts = topic.split("/");   // vm/{machineId}/events/{type}
        if (parts.length != 4) {
            log.warn("Ignoring malformed topic {}", topic);
            return;
        }
        try {
            UUID machineId = UUID.fromString(parts[1]);
            JsonNode body = mapper.readTree(message.getPayload());
            switch (parts[3]) {
                case "drop" -> publisher.publishEvent(new DropEvent(
                        machineId,
                        UUID.fromString(body.path("orderId").asText()),
                        DropEvent.Result.valueOf(body.path("result").asText("TIMEOUT"))));
                case "heartbeat" -> redis.opsForValue().set(
                        RedisKeys.heartbeat(machineId), Instant.now().toString(), Duration.ofSeconds(60));
                default -> log.debug("Unhandled event type {}", parts[3]);
            }
        } catch (Exception e) {
            log.error("Failed to process MQTT message on {}: {}", topic, e.getMessage());
        }
    }
}
