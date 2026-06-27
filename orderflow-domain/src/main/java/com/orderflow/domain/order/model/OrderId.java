package com.orderflow.domain.order.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of an {@link Order} aggregate. A typed id (rather than a raw
 * {@code UUID}) prevents accidentally passing a customer id where an order id is
 * expected — the compiler rejects it. This is "primitive obsession" avoidance.
 */
public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "order id is required");
    }

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID());
    }

    public static OrderId of(String value) {
        return new OrderId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
