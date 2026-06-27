package com.orderflow.infrastructure.order.web;

import com.orderflow.application.order.query.OrderView;

import java.util.List;

/**
 * REST representation of an order. Built from the application's {@link OrderView}
 * so the transport shape stays decoupled from both the read model and the domain.
 */
public record OrderResponse(
    String orderId,
    String customerId,
    String currency,
    String status,
    String total,
    List<Line> lines) {

    public record Line(String productId, String sku, String unitPrice, int quantity) {}

    public static OrderResponse from(OrderView view) {
        List<Line> lines = view.lines().stream()
            .map(l -> new Line(l.productId(), l.sku(), l.unitPrice(), l.quantity()))
            .toList();
        return new OrderResponse(
            view.orderId(), view.customerId(), view.currency(),
            view.status(), view.total(), lines);
    }
}
