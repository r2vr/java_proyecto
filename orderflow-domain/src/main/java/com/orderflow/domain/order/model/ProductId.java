package com.orderflow.domain.order.model;

import java.util.Objects;
import java.util.UUID;

/** Identity of a catalog product referenced by an order line. */
public record ProductId(UUID value) {

    public ProductId {
        Objects.requireNonNull(value, "product id is required");
    }

    public static ProductId of(String value) {
        return new ProductId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
