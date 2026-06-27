package com.orderflow.domain.order.exception;

import com.orderflow.domain.order.model.OrderId;
import com.orderflow.domain.order.model.OrderStatus;
import com.orderflow.domain.shared.DomainException;

/**
 * Raised when an operation is attempted that the order's current state forbids
 * (e.g. adding a line to an already confirmed order, or an illegal transition).
 */
public final class InvalidOrderStateException extends DomainException {

    public InvalidOrderStateException(String message) {
        super(message);
    }

    public static InvalidOrderStateException illegalTransition(OrderId id, OrderStatus from, OrderStatus to) {
        return new InvalidOrderStateException(
            "Order %s cannot move from %s to %s".formatted(id, from, to));
    }

    @Override
    public String code() {
        return "ORDER_INVALID_STATE";
    }
}
