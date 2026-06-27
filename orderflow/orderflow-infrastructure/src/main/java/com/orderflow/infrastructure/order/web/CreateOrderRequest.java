package com.orderflow.infrastructure.order.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST request body for creating an order. Bean Validation enforces shape at the
 * boundary so malformed input is rejected with 400 before reaching the domain.
 */
public record CreateOrderRequest(
    @NotBlank String customerId,
    @NotBlank @Size(min = 3, max = 3) String currency,
    @NotEmpty @Valid List<Line> lines) {

    public record Line(
        @NotBlank String productId,
        @NotBlank String sku,
        @NotNull @Positive BigDecimal unitPrice,
        @Positive int quantity) {}
}
