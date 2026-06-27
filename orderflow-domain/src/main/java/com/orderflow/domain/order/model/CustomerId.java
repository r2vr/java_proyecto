package com.orderflow.domain.order.model;

import java.util.Objects;
import java.util.UUID;

/** Identity of the customer who owns an order. */
public record CustomerId(UUID value) {

    public CustomerId {
        Objects.requireNonNull(value, "customer id is required");
    }

    public static CustomerId of(String value) {
        return new CustomerId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
