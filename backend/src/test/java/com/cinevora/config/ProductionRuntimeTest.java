package com.cinevora.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Only runs against an explicitly supplied, isolated PostgreSQL database. */
@EnabledIfEnvironmentVariable(named = "CINEVORA_DB_TESTS", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "debug=false", "trace=false",
        "app.cors.allowed-origin-patterns=https://cinevora.vercel.app",
        "app.media.s3.endpoint=https://storage.example.test", "app.media.s3.public-base-url=https://media.example.test",
        "app.media.s3.bucket=test", "app.media.s3.access-key=test-access", "app.media.s3.secret-key=test-secret",
        "spring.datasource.hikari.data-source-properties.sslmode=disable"
})
@ActiveProfiles("prod")
class ProductionRuntimeTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = System.getenv("CINEVORA_TEST_DATABASE_URL");
        if (url == null || !url.matches("jdbc:postgresql://[^/]+/cinevora_(ci|test)[a-zA-Z0-9_]*"))
            throw new IllegalStateException("Refusing production integration tests outside a named cinevora_ci/test database");
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> System.getenv("CINEVORA_TEST_DATABASE_USER"));
        registry.add("spring.datasource.password", () -> System.getenv("CINEVORA_TEST_DATABASE_PASSWORD"));
        registry.add("app.jwt.secret", () -> UUID.randomUUID() + "-" + UUID.randomUUID() + "abcdefghijklmnop");
    }

    @Autowired TestRestTemplate http;

    @Test void healthIsMinimalAndOperationalInternalsAreUnavailable() {
        var health = http.getForEntity("/actuator/health", String.class);
        assertEquals(200, health.getStatusCode().value());
        assertEquals("{\"status\":\"UP\"}", health.getBody());
        for (String path : new String[]{"/actuator/env", "/actuator/configprops", "/actuator/heapdump", "/swagger-ui/index.html", "/v3/api-docs"})
            assertFalse(http.getForEntity(path, String.class).getStatusCode().is2xxSuccessful(), path);
        assertEquals(404, http.getForEntity("/v3/api-docs", String.class).getStatusCode().value());
        assertEquals(404, http.getForEntity("/swagger-ui/index.html", String.class).getStatusCode().value());
    }

    @Test void anonymousMalformedJwtAndCorsAreRejected() {
        assertEquals(403, http.getForEntity("/api/v1/users/me", String.class).getStatusCode().value());
        var headers = new HttpHeaders(); headers.setBearerAuth("malformed");
        assertEquals(403, http.exchange("/api/v1/users/me", HttpMethod.GET, new HttpEntity<>(headers), String.class).getStatusCode().value());
        for (String origin : new String[]{"https://cinevora.vercel.app", "https://random-origin.invalid"}) {
            var cors = new HttpHeaders(); cors.setOrigin(origin); cors.setAccessControlRequestMethod(HttpMethod.GET);
            cors.setAccessControlRequestHeaders(java.util.List.of("Authorization", "X-Profile-Id"));
            var response = http.exchange("/api/v1/users/me", HttpMethod.OPTIONS, new HttpEntity<>(cors), String.class);
            assertEquals(origin.contains("vercel") ? 200 : 403, response.getStatusCode().value());
            assertEquals(origin.contains("vercel") ? origin : null, response.getHeaders().getAccessControlAllowOrigin());
        }
    }

    @Test void productionNeverReturnsResetToken() {
        var response = http.postForEntity("/api/v1/auth/forgot-password", Map.of("email", "admin@gmail.com"), Map.class);
        assertEquals(200, response.getStatusCode().value());
        var data = (Map<?, ?>) response.getBody().get("data");
        assertNull(data.get("developmentToken"));
    }
}
