package com.cinevora.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/** Validate before any datasource, migration or HTTP listener is created. Never echo values. */
@Configuration
public class ProductionConfigurationGuard {
    @Bean
    static BeanFactoryPostProcessor productionConfigurationCheck(Environment env) {
        return factory -> validate(env);
    }

    static void validate(Environment env) {
        boolean prod = Arrays.asList(env.getActiveProfiles()).contains("prod");
        boolean cloud = env.getProperty("RENDER") != null || env.getProperty("RAILWAY_ENVIRONMENT_ID") != null;
        if (cloud && !prod) fail("Cloud deployment requires the prod profile");
        if (!prod) return;
        if (env.getProperty("debug", Boolean.class, false) || env.getProperty("trace", Boolean.class, false))
            fail("Debug/trace logging cannot be enabled in prod");
        for (String logger : new String[]{"root", "com.cinevora", "org.springframework", "org.springframework.web", "org.springframework.security", "org.hibernate.SQL", "org.hibernate.orm.jdbc.bind", "software.amazon.awssdk"}) {
            String level = env.getProperty("logging.level." + logger, "INFO");
            if ("DEBUG".equalsIgnoreCase(level) || "TRACE".equalsIgnoreCase(level)) fail("Verbose logging cannot be enabled in prod");
        }
        if (Arrays.asList(env.getActiveProfiles()).contains("dev")) fail("Do not combine prod and dev profiles");
        String secret = required(env, "app.jwt.secret");
        String lower = secret.toLowerCase(Locale.ROOT);
        if (secret.getBytes(StandardCharsets.UTF_8).length < 48 || secret.chars().distinct().count() < 16
                || lower.contains("dev-only") || lower.contains("change-me") || lower.contains("example"))
            fail("JWT_SECRET must be a strong random secret (at least 48 bytes), not a development value");
        if (env.getProperty("app.auth.expose-development-tokens", Boolean.class, false))
            fail("Development tokens cannot be enabled in prod");
        if (!required(env, "spring.datasource.url").startsWith("jdbc:postgresql://"))
            fail("Production database URL must be a PostgreSQL JDBC URL");
        required(env, "spring.datasource.username");
        required(env, "spring.datasource.password");
        for (String origin : required(env, "app.cors.allowed-origin-patterns").split(",", -1)) {
            URI uri = https(origin.trim(), "CORS_ALLOWED_ORIGINS");
            if (origin.contains("*") || (uri.getPath() != null && !uri.getPath().isEmpty()))
                fail("Production CORS requires exact HTTPS origins without paths or wildcards");
        }
        if (!"s3".equals(env.getProperty("app.media.storage"))) fail("Production media must use persistent S3 storage");
        https(required(env, "app.media.s3.endpoint"), "MEDIA_S3_ENDPOINT");
        https(required(env, "app.media.s3.public-base-url"), "MEDIA_PUBLIC_BASE_URL");
        required(env, "app.media.s3.bucket");
        required(env, "app.media.s3.access-key");
        required(env, "app.media.s3.secret-key");
        if (env.getProperty("springdoc.api-docs.enabled", Boolean.class, true)
                || env.getProperty("springdoc.swagger-ui.enabled", Boolean.class, true))
            fail("Public Swagger/OpenAPI is disabled by the production policy");
    }

    private static String required(Environment env, String key) {
        String value = env.getProperty(key);
        if (value == null || value.isBlank() || value.contains("${")) fail("Missing required production setting: " + key);
        return value;
    }

    private static URI https(String value, String setting) {
        try {
            URI uri = URI.create(value);
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null
                    || "localhost".equalsIgnoreCase(uri.getHost()) || uri.getHost().equals("127.0.0.1"))
                fail(setting + " requires a non-local HTTPS URL");
            return uri;
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(setting + " requires a valid HTTPS URL");
        }
    }

    private static void fail(String message) { throw new IllegalStateException(message); }
}
