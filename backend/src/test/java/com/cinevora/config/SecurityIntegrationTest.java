package com.cinevora.config;

import com.cinevora.dto.AuthDtos;
import com.cinevora.entity.*;
import com.cinevora.repository.UserRepository;
import com.cinevora.repository.NotificationRepository;
import com.cinevora.security.JwtService;
import com.cinevora.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="CINEVORA_SECURITY_DB", matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class SecurityIntegrationTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        String url = System.getenv("CINEVORA_SECURITY_DATABASE_URL");
        if (url == null || !url.matches("jdbc:postgresql://[^/]+/cinevora_test_security[a-zA-Z0-9_]*"))
            throw new IllegalStateException("Security tests require an isolated named database");
        r.add("spring.datasource.url", () -> url);
        r.add("spring.datasource.username", () -> System.getenv("CINEVORA_TEST_DATABASE_USER"));
        r.add("spring.datasource.password", () -> System.getenv("CINEVORA_TEST_DATABASE_PASSWORD"));
        r.add("app.jwt.secret", () -> UUID.randomUUID() + "-" + UUID.randomUUID() + "abcdefghijklmnop");
    }
    @Autowired SessionService sessions;
    @Autowired AuthService auth;
    @Autowired UserRepository users;
    @Autowired NotificationRepository notifications;
    @Autowired JwtService jwt;
    @Autowired TestRestTemplate http;

    @Test void simultaneousRefreshHasExactlyOneWinnerAndOldTokenCannotReplay() throws Exception {
        User user = users.findByUsernameIgnoreCase("admin").orElseThrow();
        try (var pool = Executors.newFixedThreadPool(2)) {
            for (int run = 0; run < 10; run++) {
                String old = sessions.create(user, "concurrency-test", "127.0.0.1");
                var barrier = new CyclicBarrier(2);
                Callable<String> attempt = () -> {
                    barrier.await(10, TimeUnit.SECONDS);
                    try { return auth.refresh(new AuthDtos.RefreshRequest(old), null, null).refreshToken(); }
                    catch (org.springframework.security.core.AuthenticationException expected) { return null; }
                };
                Future<String> a = pool.submit(attempt), b = pool.submit(attempt);
                String first = a.get(15, TimeUnit.SECONDS), second = b.get(15, TimeUnit.SECONDS);
                assertEquals(1, (first == null ? 0 : 1) + (second == null ? 0 : 1));
                assertThrows(org.springframework.security.core.AuthenticationException.class,
                        () -> auth.refresh(new AuthDtos.RefreshRequest(old), null, null));
                var next = auth.refresh(new AuthDtos.RefreshRequest(first == null ? second : first), null, null);
                sessions.revoke(next.refreshToken());
            }
        }
    }

    @Test void cookiesCsrfAndBearerBoundaries() {
        var csrf = http.getForEntity("/api/v1/auth/csrf", Map.class);
        assertEquals(200, csrf.getStatusCode().value());
        var proof = (Map<?, ?>) csrf.getBody().get("data");
        String csrfCookie = csrf.getHeaders().getFirst("Set-Cookie").split(";", 2)[0];
        var headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Cookie", csrfCookie); headers.set((String) proof.get("headerName"), (String) proof.get("token"));
        var login = http.exchange("/api/v1/auth/login", HttpMethod.POST,
                new HttpEntity<>(Map.of("username", "admin", "password", "Cinevora@2026"), headers), Map.class);
        assertEquals(200, login.getStatusCode().value());
        Map<?, ?> data = (Map<?, ?>) login.getBody().get("data");
        assertFalse(data.containsKey("refreshToken"));
        String cookie = login.getHeaders().getFirst("Set-Cookie");
        assertTrue(cookie.contains("HttpOnly")); assertTrue(cookie.contains("SameSite=Lax"));
        assertTrue(cookie.contains("Path=/api/v1/auth")); assertFalse(cookie.contains("Domain="));
        String refreshCookie = cookie.split(";", 2)[0];
        var attack = new HttpHeaders(); attack.add("Cookie", csrfCookie + "; " + refreshCookie);
        assertEquals(403, http.exchange("/api/v1/auth/refresh", HttpMethod.POST, new HttpEntity<>(null, attack), String.class).getStatusCode().value());
        attack.setOrigin("https://attacker.invalid");
        attack.set((String) proof.get("headerName"), (String) proof.get("token"));
        assertEquals(403, http.exchange("/api/v1/auth/logout", HttpMethod.POST, new HttpEntity<>(null, attack), String.class).getStatusCode().value());
        headers.set("Cookie", csrfCookie + "; " + refreshCookie);
        assertEquals(200, http.exchange("/api/v1/auth/refresh", HttpMethod.POST, new HttpEntity<>(null, headers), Map.class).getStatusCode().value());
        assertEquals(401, http.exchange("/api/v1/auth/refresh", HttpMethod.POST, new HttpEntity<>(null, headers), Map.class).getStatusCode().value());
        var bearer = new HttpHeaders(); bearer.setBearerAuth((String) data.get("token"));
        assertEquals(200, http.exchange("/api/v1/users/me", HttpMethod.GET, new HttpEntity<>(bearer), Map.class).getStatusCode().value());
    }

    @Test void malformedClientInputIsControlledAndWrapped() {
        var missing = http.getForEntity("/api/v1/movies/suggestions", Map.class);
        assertEquals(400, missing.getStatusCode().value());
        assertEquals(false, missing.getBody().get("success"));

        var csrf = http.getForEntity("/api/v1/auth/csrf", Map.class);
        var proof = (Map<?, ?>) csrf.getBody().get("data");
        var loginHeaders = new HttpHeaders(); loginHeaders.setContentType(MediaType.APPLICATION_JSON);
        loginHeaders.add("Cookie", csrf.getHeaders().getFirst("Set-Cookie").split(";", 2)[0]);
        loginHeaders.set((String) proof.get("headerName"), (String) proof.get("token"));
        var login = http.exchange("/api/v1/auth/login", HttpMethod.POST,
                new HttpEntity<>(Map.of("username", "admin", "password", "Cinevora@2026"), loginHeaders), Map.class);
        assertEquals(200, login.getStatusCode().value());
        String token = (String) ((Map<?, ?>) login.getBody().get("data")).get("token");
        var auth = new HttpHeaders(); auth.setBearerAuth(token);
        assertEquals(400, http.exchange("/api/v1/users/me/search-history", HttpMethod.POST, new HttpEntity<>(auth), Map.class).getStatusCode().value());
        assertEquals(400, http.getForEntity("/api/v1/movies?minYear=abc", Map.class).getStatusCode().value());
        auth.setContentType(MediaType.APPLICATION_JSON);
        assertEquals(400, http.exchange("/api/v1/users/me/profiles", HttpMethod.POST, new HttpEntity<>("{", auth), Map.class).getStatusCode().value());
        auth.setContentType(MediaType.TEXT_PLAIN);
        assertEquals(415, http.exchange("/api/v1/users/me/profiles", HttpMethod.POST, new HttpEntity<>("name=test", auth), Map.class).getStatusCode().value());

    }

    @Test void adminMessagesAreAuthorizedIsolatedAndDeliveredToCustomerInbox() {
        User admin = users.findByUsernameIgnoreCase("admin").orElseThrow();
        User customer = users.findByUsernameIgnoreCase("thietthach09").orElseThrow();
        User otherCustomer = users.findByUsernameIgnoreCase("messi10").orElseThrow();
        User inactiveCustomer = users.findByUsernameIgnoreCase("cristiano07").orElseThrow();
        inactiveCustomer.setActive(false); users.save(inactiveCustomer);
        var adminHeaders = new HttpHeaders(); adminHeaders.setBearerAuth(jwt.generate(admin));
        var customerHeaders = new HttpHeaders(); customerHeaders.setBearerAuth(jwt.generate(customer));
        adminHeaders.setContentType(MediaType.APPLICATION_JSON); customerHeaders.setContentType(MediaType.APPLICATION_JSON);

        assertEquals(403, http.exchange("/api/v1/admin/notifications", HttpMethod.POST,
                new HttpEntity<>(Map.of("recipientUsername", customer.getUsername(), "broadcastToActiveCustomers", false, "title", "Private", "body", "Only this customer"), customerHeaders), Map.class).getStatusCode().value());
        assertEquals(400, http.exchange("/api/v1/admin/notifications", HttpMethod.POST,
                new HttpEntity<>(Map.of("broadcastToActiveCustomers", true, "title", "Unsafe", "body", "Rejected", "actionUrl", "javascript:alert(1)"), adminHeaders), Map.class).getStatusCode().value());
        assertEquals(200, http.exchange("/api/v1/admin/notifications", HttpMethod.POST,
                new HttpEntity<>(Map.of("recipientUsername", customer.getUsername(), "broadcastToActiveCustomers", false, "title", "Private", "body", "Only this customer", "actionUrl", "/browse"), adminHeaders), Map.class).getStatusCode().value());
        assertTrue(notifications.findTop20ByUser_IdOrderByCreatedAtDesc(customer.getId()).stream().anyMatch(n -> n.getKind().equals("ADMIN_MESSAGE") && n.getTitle().equals("Private")));
        assertFalse(notifications.findTop20ByUser_IdOrderByCreatedAtDesc(otherCustomer.getId()).stream().anyMatch(n -> n.getTitle().equals("Private")));
        var broadcast = http.exchange("/api/v1/admin/notifications", HttpMethod.POST,
                new HttpEntity<>(Map.of("broadcastToActiveCustomers", true, "title", "Broadcast", "body", "Active customers only"), adminHeaders), Map.class);
        assertEquals(200, broadcast.getStatusCode().value());
        assertEquals(9, ((Map<?, ?>) broadcast.getBody().get("data")).get("recipientCount"));
        assertTrue(notifications.findTop20ByUser_IdOrderByCreatedAtDesc(customer.getId()).stream().anyMatch(n -> n.getTitle().equals("Broadcast")));
        assertFalse(notifications.findTop20ByUser_IdOrderByCreatedAtDesc(inactiveCustomer.getId()).stream().anyMatch(n -> n.getTitle().equals("Broadcast")));
    }
}
