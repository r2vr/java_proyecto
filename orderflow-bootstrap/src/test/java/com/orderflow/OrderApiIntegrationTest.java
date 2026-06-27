package com.orderflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-slice integration test against a real PostgreSQL (Testcontainers). It
 * authenticates through the login endpoint to obtain a JWT, then drives the
 * secured order API end to end. Proves the wiring — security, web, transactions,
 * JPA, Flyway — works together, not just the units.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderApiIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    TestRestTemplate rest;

    private String token;

    @BeforeEach
    void login() {
        ResponseEntity<Map> response = rest.postForEntity(
            "/api/auth/login", Map.of("username", "demo", "password", "demo123"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        this.token = (String) response.getBody().get("token");
    }

    @Test
    void fullLifecycle_createGetConfirm() {
        ResponseEntity<Map> created = exchange("/api/orders", HttpMethod.POST, validBody());
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String id = (String) created.getBody().get("orderId");

        ResponseEntity<Map> fetched = exchange("/api/orders/" + id, HttpMethod.GET, null);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody()).containsEntry("status", "DRAFT");
        assertThat(fetched.getBody()).containsEntry("total", "39.98");

        ResponseEntity<Map> confirmed = exchange("/api/orders/" + id + "/confirm", HttpMethod.POST, null);
        assertThat(confirmed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(confirmed.getBody()).containsEntry("status", "CONFIRMED");
    }

    @Test
    void rejectsInvalidPayloadWith400() {
        Map<String, Object> invalid = Map.of("customerId", "", "currency", "EUR", "lines", new Object[]{});

        ResponseEntity<Map> response = exchange("/api/orders", HttpMethod.POST, invalid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", "VALIDATION_ERROR");
    }

    @Test
    void getUnknownOrderReturns404() {
        ResponseEntity<Map> response = exchange("/api/orders/" + UUID.randomUUID(), HttpMethod.GET, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("code", "ORDER_NOT_FOUND");
    }

    @Test
    void listReturnsCreatedOrders() {
        exchange("/api/orders", HttpMethod.POST, validBody());

        ResponseEntity<Map> page = exchange("/api/orders?page=0&size=10", HttpMethod.GET, null);

        assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) page.getBody().get("totalElements")).longValue()).isGreaterThanOrEqualTo(1);
        assertThat((java.util.List<?>) page.getBody().get("content")).isNotEmpty();
    }

    @Test
    void rejectsUnauthenticatedRequestWith401() {
        // No Authorization header -> blocked by the security filter chain.
        ResponseEntity<Map> response = rest.postForEntity("/api/orders", validBody(), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- helpers -----------------------------------------------------------

    private ResponseEntity<Map> exchange(String path, HttpMethod method, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(path, method, new HttpEntity<>(body, headers), Map.class);
    }

    private static Map<String, Object> validBody() {
        return Map.of(
            "customerId", UUID.randomUUID().toString(),
            "currency", "EUR",
            "lines", new Object[]{
                Map.of("productId", UUID.randomUUID().toString(),
                    "sku", "SKU-1", "unitPrice", 19.99, "quantity", 2)
            });
    }
}
