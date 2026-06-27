package com.orderflow.application.order.port.in;

import java.math.BigDecimal;
import java.util.List;

/**
 * Input data for creating an order, expressed in primitive/transport-friendly
 * types. The application layer translates this into domain value objects, so the
 * domain never has to know about the outside world's representation.
 */
public record CreateOrderCommand(String customerId, String currencyCode, List<Line> lines) {

    public record Line(String productId, String sku, BigDecimal unitPrice, int quantity) {}
}
