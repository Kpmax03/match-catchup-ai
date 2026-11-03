package com.matchcatchup.config;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final Pattern SUBJECT = Pattern.compile("\\\"sub\\\":\\\"([^\\\"]+)\\\"");
    @Value("${matchcatchup.jwt.secret:local-development-secret-change-this-to-a-long-random-value}") private String secret;
    @Value("${matchcatchup.jwt.expiration-seconds:86400}") private long expirationSeconds;

    public String issue(String email) {
        long exp = Instant.now().getEpochSecond() + expirationSeconds;
        String header = ENCODER.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = ENCODER.encodeToString(("{\"sub\":\"" + email.replace("\\", "\\\\").replace("\"", "\\\"") + "\",\"exp\":" + exp + "}").getBytes(StandardCharsets.UTF_8));
        String unsigned = header + "." + payload;
        return unsigned + "." + ENCODER.encodeToString(sign(unsigned));
    }

    public String validateAndGetSubject(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            String unsigned = parts[0] + "." + parts[1];
            if (!java.security.MessageDigest.isEqual(sign(unsigned), DECODER.decode(parts[2]))) return null;
            String payload = new String(DECODER.decode(parts[1]), StandardCharsets.UTF_8);
            Matcher subject = SUBJECT.matcher(payload);
            Matcher expiry = Pattern.compile("\\\"exp\\\":(\\d+)").matcher(payload);
            if (!subject.find() || !expiry.find() || Long.parseLong(expiry.group(1)) <= Instant.now().getEpochSecond()) return null;
            return subject.group(1);
        } catch (RuntimeException ex) { return null; }
    }

    private byte[] sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) { throw new IllegalStateException("Unable to sign token", ex); }
    }
}
