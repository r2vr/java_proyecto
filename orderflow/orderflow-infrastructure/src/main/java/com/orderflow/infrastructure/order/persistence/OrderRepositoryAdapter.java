package com.orderflow.infrastructure.order.persistence;

import com.orderflow.domain.order.model.Order;
import com.orderflow.domain.order.model.OrderId;
import com.orderflow.domain.order.port.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Driven adapter that fulfils the domain {@link OrderRepository} port using Spring
 * Data JPA. The domain depends on the port; this class depends on the domain.
 * That inversion is what keeps the database swappable.
 */
@Repository
public class OrderRepositoryAdapter implements OrderRepository {

    private final SpringDataOrderRepository jpa;

    public OrderRepositoryAdapter(SpringDataOrderRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(Order order) {
        // Update an existing aggregate by mutating the managed entity, so JPA
        // applies the change under the @Version optimistic-lock check. New
        // aggregates are inserted with their full graph. (Slice 2 only mutates
        // status; when lines become mutable, sync them here too.)
        jpa.findById(order.id().value()).ifPresentOrElse(
            existing -> existing.setStatus(order.status().name()),
            () -> jpa.save(OrderEntityMapper.toEntity(order)));
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return jpa.findById(id.value()).map(OrderEntityMapper::toDomain);
    }
}
