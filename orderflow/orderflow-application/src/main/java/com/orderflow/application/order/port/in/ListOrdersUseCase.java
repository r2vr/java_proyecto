package com.orderflow.application.order.port.in;

import com.orderflow.application.order.query.OrderSummary;
import com.orderflow.application.order.query.Page;

/** Input port for listing orders, optionally filtered by status. */
public interface ListOrdersUseCase {

    Page<OrderSummary> handle(int page, int size, String status);
}
