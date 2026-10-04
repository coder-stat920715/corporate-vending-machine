package com.corporate.vending.adapter.out.gateway;

import com.corporate.vending.application.port.out.UpiGatewayPort;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UpiGatewayAdapter implements UpiGatewayPort {

    private final RestClient client;

    public UpiGatewayAdapter(RestClient.Builder builder,
                             @Value("${vending.gateway.upi.base-url}") String baseUrl,
                             @Value("${vending.gateway.upi.api-key}") String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        this.client = builder.requestFactory(factory)
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    @Override
    public QrResponse createDynamicQr(UUID orderId, long amountPaise) {
        JsonNode body = client.post().uri("/v1/qr")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("reference", orderId.toString(), "amountPaise", amountPaise))
                .retrieve()
                .body(JsonNode.class);
        if (body == null || body.path("txnId").asText("").isBlank()) {
            throw new IllegalStateException("UPI gateway returned an empty QR response");
        }
        return new QrResponse(body.path("txnId").asText(), body.path("qrPayload").asText());
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
