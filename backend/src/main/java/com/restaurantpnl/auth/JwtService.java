package com.restaurantpnl.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService {
    private final byte[] secret;
    private final String issuer;

    public JwtService(
            @Value("${restaurant-pnl.jwt-secret}") String secret,
            @Value("${restaurant-pnl.jwt-issuer}") String issuer
    ) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.issuer = issuer;
    }

    public String issue(AppUser user) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", issuer);
        payload.put("sub", user.id());
        payload.put("email", user.email());
        payload.put("name", user.name());
        payload.put("iat", now);
        payload.put("exp", now + 60 * 60 * 12);

        String header = encodeJson("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String body = encodeJson(toJson(payload));
        String signature = sign(header + "." + body);
        return header + "." + body + "." + signature;
    }

    public Optional<String> subject(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }

        String expectedSignature = sign(parts[0] + "." + parts[1]);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            return Optional.empty();
        }

        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        long exp = longClaim(payload, "exp").orElse(0L);
        if (exp < Instant.now().getEpochSecond()) {
            return Optional.empty();
        }

        return stringClaim(payload, "sub");
    }

    private String encodeJson(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign token", exception);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        if (leftBytes.length != rightBytes.length) {
            return false;
        }

        int result = 0;
        for (int index = 0; index < leftBytes.length; index++) {
            result |= leftBytes[index] ^ rightBytes[index];
        }
        return result == 0;
    }

    private String toJson(Map<String, Object> payload) {
        StringBuilder json = new StringBuilder("{");
        int index = 0;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (index++ > 0) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":");
            if (entry.getValue() instanceof Number number) {
                json.append(number);
            } else {
                json.append("\"").append(escape(String.valueOf(entry.getValue()))).append("\"");
            }
        }
        return json.append("}").toString();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Optional<String> stringClaim(String payload, String claim) {
        String marker = "\"" + claim + "\":\"";
        int start = payload.indexOf(marker);
        if (start < 0) {
            return Optional.empty();
        }
        int valueStart = start + marker.length();
        int valueEnd = payload.indexOf("\"", valueStart);
        return valueEnd < 0 ? Optional.empty() : Optional.of(payload.substring(valueStart, valueEnd));
    }

    private Optional<Long> longClaim(String payload, String claim) {
        String marker = "\"" + claim + "\":";
        int start = payload.indexOf(marker);
        if (start < 0) {
            return Optional.empty();
        }
        int valueStart = start + marker.length();
        int valueEnd = valueStart;
        while (valueEnd < payload.length() && Character.isDigit(payload.charAt(valueEnd))) {
            valueEnd++;
        }
        return Optional.of(Long.parseLong(payload.substring(valueStart, valueEnd)));
    }
}
