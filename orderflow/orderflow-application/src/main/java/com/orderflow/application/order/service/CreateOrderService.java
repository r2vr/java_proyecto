package com.orderflow.application.order.service;

import com.orderflow.application.order.port.in.CreateOrderCommand;
import com.orderflow.application.order.port.in.CreateOrderUseCase;
import com.orderflow.application.order.port.out.DomainEventPublisher;
import com.orderflow.domain.order.model.*;
import com.orderflow.domain.order.port.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Currency;
import java.util.Objects;

/**
 * Orchestrates the creation of an order: it maps the command into domain types,
 * lets the {@link Order} aggregate enforce the business rules, persists it
 * through the {@link OrderRepository} port and publishes the recorded events.
 * <p>
 * Constructor injection keeps it unit-testable with fakes; Spring discovers it by
 * component scan. The single write makes the transaction trivial, but it is
 * declared explicitly for consistency with the other use cases.
 */
@Service
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public CreateOrderService(OrderRepository orderRepository,
                              DomainEventPublisher eventPublisher,
                              Clock clock) {
        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    @Transactional
    public OrderId handle(CreateOrderCommand command) {
        Currency currency = Currency.getInstance(command.currencyCode());
        Order order = Order.create(
            OrderId.newId(),
            CustomerId.of(command.customerId()),
            currency,
            clock);

        for (CreateOrderCommand.Line line : command.lines()) {
            order.addLine(new OrderLine(
                ProductId.of(line.productId()),
                Sku.of(line.sku()),
                new Money(line.unitPrice(), currency),
                Quantity.of(line.quantity())));
        }

        orderRepository.save(order);
        eventPublisher.publish(order.pullDomainEvents());
        return order.id();
    }
}
