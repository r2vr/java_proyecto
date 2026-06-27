package com.orderflow.application.order.port.in;

import com.orderflow.domain.order.model.OrderId;

/**
 * Input port (driving side) for the "create order" use case. The web adapter
 * depends on this interface, not on the concrete service — so the delivery
 * mechanism (REST today, messaging tomorrow) stays decoupled from orchestration.
 */
public interface CreateOrderUseCase {

    OrderId handle(CreateOrderCommand command);
}
