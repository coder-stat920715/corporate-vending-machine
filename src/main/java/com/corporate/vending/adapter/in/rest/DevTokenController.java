package com.corporate.vending.adapter.in.rest;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Local development only: issues a signed JWT so Swagger/curl can call secured endpoints. Not available in prod. */
@Profile("dev")
@RestController
@RequestMapping("/dev")
public class DevTokenController {

    private final NimbusJwtEncoder encoder;

    public DevTokenController(@Value("${vending.security.jwt-secret}") String secret) {
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
    }

    @PostMapping("/token")
    public Map<String, String> token(@RequestParam(defaultValue = "dev-user") String subject,
                                     @RequestParam(defaultValue = "EMPLOYEE") String roles) {
        Instant now = Instant.now();
        List<String> roleList = Arrays.stream(roles.split(",")).map(String::trim).toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("roles", roleList)
                .build();
        String value = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return Map.of("access_token", value, "token_type", "Bearer");
    }
}
