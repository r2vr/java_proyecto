package com.orderflow.domain.order.model;

/**
 * Stock Keeping Unit: the human-readable product code captured at the time the
 * line was added, so historical orders stay readable even if the catalog changes.
 */
public record Sku(String value) {

    public Sku {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SKU must not be blank");
        }
    }

    public static Sku of(String value) {
        return new Sku(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
