package com.orderflow.infrastructure.order.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Spring Data JPA repository for {@link OrderEntity}. */
public interface SpringDataOrderRepository extends JpaRepository<OrderEntity, UUID> {

    Page<OrderEntity> findByStatus(String status, Pageable pageable);
}
