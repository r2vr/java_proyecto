package com.orderflow.domain.order.port;

import com.orderflow.domain.order.model.Order;
import com.orderflow.domain.order.model.OrderId;

import java.util.Optional;

/**
 * Output port for order persistence.
 * <p>
 * Declared in the domain and implemented by an infrastructure adapter (JPA,
 * in-memory...). This dependency inversion is the heart of hexagonal
 * architecture: the domain depends on an abstraction it owns, never on a
 * database technology. {@link Optional} makes "not found" explicit instead of
 * returning {@code null}.
 */
public interface OrderRepository {

    void save(Order order);

    Optional<Order> findById(OrderId id);
}
