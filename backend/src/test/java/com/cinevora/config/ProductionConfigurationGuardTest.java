package com.cinevora.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductionConfigurationGuardTest {
    private MockEnvironment valid() {
        var env = new MockEnvironment();
        env.setActiveProfiles("prod");
        return env.withProperty("app.jwt.secret", UUID.randomUUID() + "-" + UUID.randomUUID() + "abcdefghijklmnop")
                .withProperty("spring.datasource.url", "jdbc:postgresql://db.test:5432/cinevora_test")
                .withProperty("spring.datasource.username", "test")
                .withProperty("spring.datasource.password", "test-only")
                .withProperty("app.cors.allowed-origin-patterns", "https://cinevora.vercel.app")
                .withProperty("app.media.storage", "s3")
                .withProperty("app.media.s3.endpoint", "https://storage.example.test")
                .withProperty("app.media.s3.public-base-url", "https://media.example.test")
                .withProperty("app.media.s3.bucket", "test")
                .withProperty("app.media.s3.access-key", "test")
                .withProperty("app.media.s3.secret-key", "test-only")
                .withProperty("springdoc.api-docs.enabled", "false")
                .withProperty("springdoc.swagger-ui.enabled", "false");
    }

    @Test void secureConfigurationPassesAndDevRemainsConvenient() {
        assertDoesNotThrow(() -> ProductionConfigurationGuard.validate(valid()));
        assertDoesNotThrow(() -> ProductionConfigurationGuard.validate(new MockEnvironment()));
    }

    @Test void cloudCannotDefaultToDev() {
        assertThrows(IllegalStateException.class, () -> ProductionConfigurationGuard.validate(new MockEnvironment().withProperty("RENDER", "true")));
        var env = valid(); env.setActiveProfiles("prod", "dev");
        assertThrows(IllegalStateException.class, () -> ProductionConfigurationGuard.validate(env));
    }

    @Test void missingWeakAndKnownSecretsFailWithoutEchoingSecret() {
        for (String value : new String[]{"", "short", "a".repeat(64)}) {
            var failure = assertThrows(IllegalStateException.class, () -> ProductionConfigurationGuard.validate(valid().withProperty("app.jwt.secret", value)));
            if (!value.isEmpty()) assertFalse(failure.getMessage().contains(value));
        }
    }

    @Test void wildcardLocalHttpAndMalformedCorsFail() {
        for (String origin : new String[]{"*", "https://*.vercel.app", "http://localhost:5173", "https://localhost", "https://site.test/path", "https://site.test,", "https://user:pass@site.test"})
            assertThrows(IllegalStateException.class, () -> ProductionConfigurationGuard.validate(valid().withProperty("app.cors.allowed-origin-patterns", origin)));
    }

    @Test void tokensSwaggerAndLocalMediaFail() {
        for (String key : new String[]{"debug", "trace", "app.auth.expose-development-tokens", "springdoc.api-docs.enabled", "springdoc.swagger-ui.enabled"})
            assertThrows(IllegalStateException.class, () -> ProductionConfigurationGuard.validate(valid().withProperty(key, "true")));
        assertThrows(IllegalStateException.class, () -> ProductionConfigurationGuard.validate(valid().withProperty("app.media.storage", "local")));
        assertThrows(IllegalStateException.class, () -> ProductionConfigurationGuard.validate(valid().withProperty("app.media.s3.public-base-url", "http://localhost:9000")));
    }
}
