package com.orderflow.infrastructure.order.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA mapping of the order aggregate. This is a persistence detail kept apart
 * from the domain {@code Order}: the model stays free of JPA annotations, and the
 * mapper bridges the two. {@code @Version} gives optimistic locking, so two
 * concurrent updates can't silently overwrite each other.
 */
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderLineEntity> lines = new ArrayList<>();

    protected OrderEntity() {
        // required by JPA
    }

    public OrderEntity(UUID id, UUID customerId, String currency, String status, Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void addLine(OrderLineEntity line) {
        line.setOrder(this);
        lines.add(line);
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }
    public List<OrderLineEntity> getLines() { return lines; }
}
