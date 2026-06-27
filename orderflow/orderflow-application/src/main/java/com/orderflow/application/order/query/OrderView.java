package com.orderflow.application.order.query;

import com.orderflow.domain.order.model.Order;
import com.orderflow.domain.order.model.OrderLine;

import java.util.List;

/**
 * Read model returned by query use cases. A view dedicated to the outside world
 * keeps the domain aggregate from leaking through the API and lets the response
 * shape evolve independently of the model.
 */
public record OrderView(
    String orderId,
    String customerId,
    String currency,
    String status,
    String total,
    List<Line> lines) {

    public record Line(String productId, String sku, String unitPrice, int quantity) {}

    /** Projects a domain {@link Order} into its view representation. */
    public static OrderView from(Order order) {
        List<Line> lines = order.lines().stream()
            .map(OrderView::toLine)
            .toList();
        return new OrderView(
            order.id().toString(),
            order.customerId().toString(),
            order.currency().getCurrencyCode(),
            order.status().name(),
            order.total().amount().toPlainString(),
            lines);
    }

    private static Line toLine(OrderLine line) {
        return new Line(
            line.productId().toString(),
            line.sku().value(),
            line.unitPrice().amount().toPlainString(),
            line.quantity().value());
    }
}
