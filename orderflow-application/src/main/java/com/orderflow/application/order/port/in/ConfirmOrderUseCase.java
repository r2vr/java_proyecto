package com.orderflow.application.order.port.in;

import com.orderflow.application.order.query.OrderView;
import com.orderflow.domain.order.model.OrderId;

/** Input port for confirming a draft order. */
public interface ConfirmOrderUseCase {

    OrderView handle(OrderId orderId);
}
