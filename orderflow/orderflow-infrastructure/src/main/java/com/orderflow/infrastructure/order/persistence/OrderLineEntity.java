package com.orderflow.infrastructure.order.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

/** JPA mapping of a single order line, owned by {@link OrderEntity}. */
@Entity
@Table(name = "order_lines")
public class OrderLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(name = "unit_price_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPriceAmount;

    @Column(nullable = false)
    private int quantity;

    protected OrderLineEntity() {
    }

    public OrderLineEntity(UUID productId, String sku, BigDecimal unitPriceAmount, int quantity) {
        this.productId = productId;
        this.sku = sku;
        this.unitPriceAmount = unitPriceAmount;
        this.quantity = quantity;
    }

    void setOrder(OrderEntity order) { this.order = order; }

    public UUID getProductId() { return productId; }
    public String getSku() { return sku; }
    public BigDecimal getUnitPriceAmount() { return unitPriceAmount; }
    public int getQuantity() { return quantity; }
}
