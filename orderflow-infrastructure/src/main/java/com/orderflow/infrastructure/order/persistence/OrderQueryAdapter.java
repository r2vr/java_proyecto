package com.orderflow.infrastructure.order.persistence;

import com.orderflow.application.order.port.out.OrderQueryPort;
import com.orderflow.application.order.query.OrderSummary;
import com.orderflow.application.order.query.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Read-side adapter implementing {@link OrderQueryPort}. Reuses the entity→domain
 * mapper to project each row into a summary, and translates Spring Data's page
 * into the application's own {@link Page} type.
 */
@Component
public class OrderQueryAdapter implements OrderQueryPort {

    private final SpringDataOrderRepository jpa;

    public OrderQueryAdapter(SpringDataOrderRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Page<OrderSummary> list(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        org.springframework.data.domain.Page<OrderEntity> result =
            (status == null || status.isBlank())
                ? jpa.findAll(pageable)
                : jpa.findByStatus(status.toUpperCase(), pageable);

        List<OrderSummary> content = result.getContent().stream()
            .map(OrderEntityMapper::toDomain)
            .map(OrderSummary::from)
            .toList();

        return new Page<>(content, result.getNumber(), result.getSize(),
            result.getTotalElements(), result.getTotalPages());
    }
}
