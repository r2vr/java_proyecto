package com.orderflow.domain.order.model;

import java.util.Objects;

/**
 * A single line within an order: a product, the quantity ordered and the unit
 * price captured at the moment it was added (prices change; the order must not).
 * <p>
 * Immutable: changing a line means replacing it, which keeps the {@link Order}
 * aggregate the sole guardian of mutation rules.
 */
public record OrderLine(ProductId productId, Sku sku, Money unitPrice, Quantity quantity) {

    public OrderLine {
        Objects.requireNonNull(productId, "productId is required");
        Objects.requireNonNull(sku, "sku is required");
        Objects.requireNonNull(unitPrice, "unitPrice is required");
        Objects.requireNonNull(quantity, "quantity is required");
        if (unitPrice.isNegative()) {
            throw new IllegalArgumentException("unitPrice cannot be negative");
        }
    }

    /** The line's contribution to the order total. */
    public Money subtotal() {
        return unitPrice.multiply(quantity.value());
    }
}
