package com.orderflow.domain.order.event;

import com.orderflow.domain.order.model.Money;
import com.orderflow.domain.order.model.OrderId;
import com.orderflow.domain.shared.DomainEvent;

import java.time.Instant;

/**
 * The closed set of facts an order can emit. A {@code sealed} hierarchy lets
 * consumers {@code switch} over events with exhaustiveness checked by the
 * compiler — add a new event and every handler that forgot to deal with it fails
 * to compile.
 */
public sealed interface OrderEvent extends DomainEvent
        permits OrderEvent.OrderCreated, OrderEvent.OrderConfirmed, OrderEvent.OrderCancelled {

    OrderId orderId();

    record OrderCreated(OrderId orderId, Instant occurredAt) implements OrderEvent {}

    record OrderConfirmed(OrderId orderId, Money total, Instant occurredAt) implements OrderEvent {}

    record OrderCancelled(OrderId orderId, String reason, Instant occurredAt) implements OrderEvent {}
}
