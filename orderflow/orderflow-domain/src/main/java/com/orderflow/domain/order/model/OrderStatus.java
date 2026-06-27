package com.orderflow.domain.order.model;

import java.util.Set;

/**
 * Lifecycle of an order, modelled as a state machine. Each state declares which
 * states it may transition to, so the legal flow lives in one place and the
 * aggregate can reject illegal moves uniformly.
 *
 * <pre>
 *   DRAFT ──confirm──▶ CONFIRMED ──pay──▶ PAID ──ship──▶ SHIPPED
 *     │                    │
 *     └──────cancel────────┴──────────────▶ CANCELLED
 * </pre>
 */
public enum OrderStatus {

    DRAFT,
    CONFIRMED,
    PAID,
    SHIPPED,
    CANCELLED;

    static {
        DRAFT.allowed = Set.of(CONFIRMED, CANCELLED);
        CONFIRMED.allowed = Set.of(PAID, CANCELLED);
        PAID.allowed = Set.of(SHIPPED);
        SHIPPED.allowed = Set.of();
        CANCELLED.allowed = Set.of();
    }

    private Set<OrderStatus> allowed;

    public boolean canTransitionTo(OrderStatus target) {
        return allowed.contains(target);
    }

    public boolean isTerminal() {
        return allowed.isEmpty();
    }
}
