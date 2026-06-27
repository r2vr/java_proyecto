package com.orderflow.application.order.service;

import com.orderflow.application.order.port.in.GetOrderUseCase;
import com.orderflow.application.order.query.OrderView;
import com.orderflow.domain.order.exception.OrderNotFoundException;
import com.orderflow.domain.order.model.OrderId;
import com.orderflow.domain.order.port.OrderRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads a single order and projects it into an {@link OrderView}. Marked
 * {@code readOnly} so the persistence layer can skip dirty-checking and flushing.
 */
@Service
public class GetOrderService implements GetOrderUseCase {

    private final OrderRepository orderRepository;

    public GetOrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Cacheable(cacheNames = "orders", key = "#orderId.value()")
    @Transactional(readOnly = true)
    public OrderView handle(OrderId orderId) {
        return orderRepository.findById(orderId)
            .map(OrderView::from)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
