package com.orderflow.application.order.query;

import com.orderflow.domain.order.model.Order;

/** Lightweight projection for list views (no line detail). */
public record OrderSummary(String orderId, String customerId, String currency, String status, String total) {

    public static OrderSummary from(Order order) {
        return new OrderSummary(
            order.id().toString(),
            order.customerId().toString(),
            order.currency().getCurrencyCode(),
            order.status().name(),
            order.total().amount().toPlainString());
    }
}
