package com.orderflow.domain.order.model;

import com.orderflow.domain.order.event.OrderEvent;
import com.orderflow.domain.order.exception.InvalidOrderStateException;
import com.orderflow.domain.shared.DomainEvent;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root of the ordering context.
 * <p>
 * The {@code Order} is the single entry point for any change to its lines and
 * status: callers never mutate an {@link OrderLine} directly. This guarantees
 * that every business invariant is enforced in one place:
 * <ul>
 *   <li>lines can only be added while the order is a {@link OrderStatus#DRAFT};</li>
 *   <li>an order can only be confirmed if it has at least one line;</li>
 *   <li>status changes must follow the {@link OrderStatus} state machine;</li>
 *   <li>all lines share the order's currency.</li>
 * </ul>
 * <p>
 * Time is injected via {@link Clock} rather than read from {@code Instant.now()}
 * so behaviour is deterministic and testable. Mutating operations record
 * {@link DomainEvent}s that the application layer pulls and publishes.
 */
public final class Order {

    private final OrderId id;
    private final CustomerId customerId;
    private final Currency currency;
    private final List<OrderLine> lines = new ArrayList<>();
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    private OrderStatus status;
    private final Instant createdAt;

    private Order(OrderId id, CustomerId customerId, Currency currency, Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.currency = currency;
        this.status = OrderStatus.DRAFT;
        this.createdAt = createdAt;
    }

    /** Creates a new empty draft order and records an {@code OrderCreated} event. */
    public static Order create(OrderId id, CustomerId customerId, Currency currency, Clock clock) {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(customerId, "customerId is required");
        Objects.requireNonNull(currency, "currency is required");
        Instant now = Instant.now(clock);
        Order order = new Order(id, customerId, currency, now);
        order.domainEvents.add(new OrderEvent.OrderCreated(id, now));
        return order;
    }

    /**
     * Rehydrates an order from persisted state without firing events. Used by the
     * persistence adapter when mapping a database row back into the model.
     */
    public static Order rehydrate(OrderId id, CustomerId customerId, Currency currency,
                                  OrderStatus status, List<OrderLine> lines, Instant createdAt) {
        Order order = new Order(id, customerId, currency, createdAt);
        order.status = status;
        order.lines.addAll(lines);
        return order;
    }

    public void addLine(OrderLine line) {
        requireStatus(OrderStatus.DRAFT, "add lines");
        if (!line.unitPrice().currency().equals(currency)) {
            throw new InvalidOrderStateException(
                "Line currency %s does not match order currency %s"
                    .formatted(line.unitPrice().currency().getCurrencyCode(), currency.getCurrencyCode()));
        }
        lines.add(line);
    }

    public void confirm(Clock clock) {
        if (lines.isEmpty()) {
            throw new InvalidOrderStateException("Cannot confirm an order with no lines");
        }
        transitionTo(OrderStatus.CONFIRMED);
        domainEvents.add(new OrderEvent.OrderConfirmed(id, total(), Instant.now(clock)));
    }

    public void cancel(String reason, Clock clock) {
        transitionTo(OrderStatus.CANCELLED);
        domainEvents.add(new OrderEvent.OrderCancelled(id, reason, Instant.now(clock)));
    }

    /** Sum of every line subtotal. Empty orders total zero in the order currency. */
    public Money total() {
        return lines.stream()
            .map(OrderLine::subtotal)
            .reduce(Money.zero(currency), Money::add);
    }

    private void transitionTo(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw InvalidOrderStateException.illegalTransition(id, status, target);
        }
        this.status = target;
    }

    private void requireStatus(OrderStatus expected, String action) {
        if (status != expected) {
            throw new InvalidOrderStateException(
                "Cannot %s while order is %s (must be %s)".formatted(action, status, expected));
        }
    }

    /** Returns and clears the recorded events, transferring ownership to the caller. */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> pulled = List.copyOf(domainEvents);
        domainEvents.clear();
        return pulled;
    }

    public OrderId id() {
        return id;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public Currency currency() {
        return currency;
    }

    public OrderStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    /** Unmodifiable view: callers cannot bypass {@link #addLine} to mutate lines. */
    public List<OrderLine> lines() {
        return Collections.unmodifiableList(lines);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Order other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
