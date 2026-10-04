package com.corporate.vending.adapter.in.rest;

import com.corporate.vending.application.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks")
public class WebhookController {

    private final OrderService orderService;
    private final ObjectMapper mapper;

    @Value("${vending.gateway.upi.webhook-secret}")
    private String webhookSecret;

    @PostMapping("/upi")
    public ResponseEntity<Void> upi(@RequestHeader("X-Signature") String signature, @RequestBody String rawBody)
            throws Exception {
        if (!validSignature(rawBody, signature)) {
            return ResponseEntity.status(401).build();
        }
        JsonNode json = mapper.readTree(rawBody);
        orderService.onUpiPaymentResult(
                UUID.fromString(json.path("orderId").asText()),
                json.path("txnId").asText(),
                "SUCCESS".equalsIgnoreCase(json.path("status").asText()));
        return ResponseEntity.ok().build();
    }

    private boolean validSignature(String body, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    signature.toLowerCase().getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return false;
        }
    }
}
