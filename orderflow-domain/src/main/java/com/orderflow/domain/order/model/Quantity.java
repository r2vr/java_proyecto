package com.orderflow.domain.order.model;

/**
 * A strictly positive count of units for an order line.
 * <p>
 * A dedicated type (instead of a bare {@code int}) makes the "must be &gt; 0"
 * rule impossible to forget and self-documents method signatures.
 */
public record Quantity(int value) {

    public Quantity {
        if (value <= 0) {
            throw new IllegalArgumentException("Quantity must be positive, was " + value);
        }
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }
}
