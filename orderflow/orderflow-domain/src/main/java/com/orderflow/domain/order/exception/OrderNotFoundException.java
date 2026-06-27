package com.orderflow.domain.order.exception;

import com.orderflow.domain.order.model.OrderId;
import com.orderflow.domain.shared.DomainException;

/** Raised by adapters when an order id does not resolve to a stored aggregate. */
public final class OrderNotFoundException extends DomainException {

    public OrderNotFoundException(OrderId id) {
        super("Order not found: " + id);
    }

    @Override
    public String code() {
        return "ORDER_NOT_FOUND";
    }
}
