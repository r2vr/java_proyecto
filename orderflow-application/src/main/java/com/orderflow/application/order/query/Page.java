package com.orderflow.application.order.query;

import java.util.List;

/**
 * Transport-friendly page of results. A small owned type (instead of Spring
 * Data's {@code Page}) keeps the application layer free of persistence types.
 */
public record Page<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
}
