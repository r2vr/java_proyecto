package com.orderflow.application.order.service;

import com.orderflow.application.order.port.in.ConfirmOrderUseCase;
import com.orderflow.application.order.port.out.DomainEventPublisher;
import com.orderflow.application.order.query.OrderView;
import com.orderflow.domain.order.exception.OrderNotFoundException;
import com.orderflow.domain.order.model.Order;
import com.orderflow.domain.order.model.OrderId;
import com.orderflow.domain.order.port.OrderRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Confirms an order. The whole read-modify-write runs in a single transaction
 * ({@code @Transactional}): the aggregate is loaded, the domain enforces the
 * transition, and the change is flushed under the entity's optimistic-lock
 * version. If a concurrent transaction confirmed the same order first, the commit
 * fails instead of silently overwriting.
 */
@Service
public class ConfirmOrderService implements ConfirmOrderUseCase {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public ConfirmOrderService(OrderRepository orderRepository,
                               DomainEventPublisher eventPublisher,
                               Clock clock) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "orders", key = "#orderId.value()")
    public OrderView handle(OrderId orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.confirm(clock);

        orderRepository.save(order);
        eventPublisher.publish(order.pullDomainEvents());
        return OrderView.from(order);
    }
}
