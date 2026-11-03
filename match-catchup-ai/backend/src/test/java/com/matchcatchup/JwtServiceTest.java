package com.matchcatchup;

import com.matchcatchup.config.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    @Test void signsAndValidatesBearerTokens() {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", "unit-test-signing-secret-that-is-long-enough");
        ReflectionTestUtils.setField(service, "expirationSeconds", 3600L);
        String token = service.issue("fan@example.com");
        assertEquals("fan@example.com", service.validateAndGetSubject(token));
        assertNull(service.validateAndGetSubject(token.substring(0, token.length() - 1) + "a"));
    }
}
