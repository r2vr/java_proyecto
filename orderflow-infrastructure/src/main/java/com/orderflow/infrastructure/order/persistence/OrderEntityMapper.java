package com.orderflow.infrastructure.order.persistence;

import com.orderflow.domain.order.model.*;

import java.util.Currency;
import java.util.List;

/**
 * Translates between the domain {@link Order} aggregate and its JPA
 * representation. Keeping this mapping explicit (rather than annotating the
 * domain with JPA) is what lets the model stay persistence-ignorant.
 */
final class OrderEntityMapper {

    private OrderEntityMapper() {
    }

    static OrderEntity toEntity(Order order) {
        OrderEntity entity = new OrderEntity(
            order.id().value(),
            order.customerId().value(),
            order.currency().getCurrencyCode(),
            order.status().name(),
            order.createdAt());

        for (OrderLine line : order.lines()) {
            entity.addLine(new OrderLineEntity(
                line.productId().value(),
                line.sku().value(),
                line.unitPrice().amount(),
                line.quantity().value()));
        }
        return entity;
    }

    static Order toDomain(OrderEntity entity) {
        Currency currency = Currency.getInstance(entity.getCurrency());
        List<OrderLine> lines = entity.getLines().stream()
            .map(line -> new OrderLine(
                new ProductId(line.getProductId()),
                Sku.of(line.getSku()),
                new Money(line.getUnitPriceAmount(), currency),
                Quantity.of(line.getQuantity())))
            .toList();

        return Order.rehydrate(
            new OrderId(entity.getId()),
            new CustomerId(entity.getCustomerId()),
            currency,
            OrderStatus.valueOf(entity.getStatus()),
            lines,
            entity.getCreatedAt());
    }
}
