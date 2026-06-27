package com.orderflow.application.order.port.in;

import com.orderflow.application.order.query.OrderView;
import com.orderflow.domain.order.model.OrderId;

/** Input port for reading a single order. */
public interface GetOrderUseCase {

    OrderView handle(OrderId orderId);
}
