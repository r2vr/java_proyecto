package com.orderflow.infrastructure.order.web;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error payload. {@code code} is the stable, machine-readable identifier
 * (from the domain or a validation constant); {@code details} carries field-level
 * messages for validation failures.
 */
public record ApiError(String code, String message, List<String> details, Instant timestamp) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of(), Instant.now());
    }

    public static ApiError of(String code, String message, List<String> details) {
        return new ApiError(code, message, details, Instant.now());
    }
}
