package com.corporate.vending.adapter.out.gateway;

import com.corporate.vending.application.port.out.CardGatewayPort;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class CardGatewayAdapter implements CardGatewayPort {

    private final RestClient client;

    public CardGatewayAdapter(RestClient.Builder builder,
                              @Value("${vending.gateway.card.base-url}") String baseUrl,
                              @Value("${vending.gateway.card.api-key}") String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(8000);
        this.client = builder.requestFactory(factory)
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    @Override
    public Optional<String> charge(String cardToken, long amountPaise, UUID orderId) {
        try {
            JsonNode body = client.post().uri("/v1/charges")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", orderId.toString())
                    .body(Map.of("token", cardToken, "amountPaise", amountPaise, "reference", orderId.toString()))
                    .retrieve()
                    .body(JsonNode.class);
            if (body != null && "APPROVED".equalsIgnoreCase(body.path("status").asText())) {
                return Optional.of(body.path("txnId").asText());
            }
            return Optional.empty();
        } catch (HttpClientErrorException e) {
            return Optional.empty();
        }
    }

    @Override
    public void refund(String txnId, long amountPaise) {
        client.post().uri("/v1/refunds")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("txnId", txnId, "amountPaise", amountPaise))
                .retrieve()
                .toBodilessEntity();
    }
}
