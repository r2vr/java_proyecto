package com.orderflow.application.order.port.out;

import com.orderflow.application.order.query.OrderSummary;
import com.orderflow.application.order.query.Page;

/**
 * Read-side port (CQRS-style): kept separate from the write {@code OrderRepository}
 * so queries can evolve and be optimised independently of the aggregate.
 */
public interface OrderQueryPort {

    Page<OrderSummary> list(int page, int size, String status);
}
