package com.orderflow.domain.order.model;

import com.orderflow.domain.order.event.OrderEvent;
import com.orderflow.domain.order.exception.InvalidOrderStateException;
import com.orderflow.domain.shared.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioural tests for the {@link Order} aggregate. They read as specifications
 * of the business rules, use a fixed {@link Clock} for determinism, and assert on
 * outcomes (state, totals, events) rather than implementation details.
 */
@DisplayName("Order aggregate")
class OrderTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-25T10:00:00Z"), ZoneOffset.UTC);

    private Order newDraft() {
        return Order.create(OrderId.newId(), new CustomerId(UUID.randomUUID()), EUR, clock);
    }

    private OrderLine line(String price, int qty) {
        return new OrderLine(new ProductId(UUID.randomUUID()), Sku.of("SKU-1"),
            Money.of(price, "EUR"), Quantity.of(qty));
    }

    @Test
    @DisplayName("starts as DRAFT and records an OrderCreated event")
    void startsAsDraft() {
        Order order = newDraft();

        assertThat(order.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(order.pullDomainEvents())
            .singleElement()
            .isInstanceOf(OrderEvent.OrderCreated.class);
    }

    @Test
    @DisplayName("computes the total from line subtotals")
    void computesTotal() {
        Order order = newDraft();
        order.addLine(line("19.99", 2)); // 39.98
        order.addLine(line("5.00", 3));  // 15.00

        assertThat(order.total()).isEqualTo(Money.of("54.98", "EUR"));
    }

    @Nested
    @DisplayName("confirmation")
    class Confirmation {

        @Test
        @DisplayName("moves a non-empty draft to CONFIRMED and emits OrderConfirmed")
        void confirmsNonEmpty() {
            Order order = newDraft();
            order.addLine(line("10.00", 1));
            order.pullDomainEvents(); // discard creation event

            order.confirm(clock);

            assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(order.pullDomainEvents())
                .singleElement()
                .isInstanceOf(OrderEvent.OrderConfirmed.class);
        }

        @Test
        @DisplayName("rejects confirming an empty order")
        void rejectsEmpty() {
            Order order = newDraft();

            assertThatThrownBy(() -> order.confirm(clock))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("no lines");
        }
    }

    @Test
    @DisplayName("forbids adding lines once confirmed")
    void locksAfterConfirm() {
        Order order = newDraft();
        order.addLine(line("10.00", 1));
        order.confirm(clock);

        assertThatThrownBy(() -> order.addLine(line("1.00", 1)))
            .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    @DisplayName("rejects lines in a different currency")
    void rejectsForeignCurrency() {
        Order order = newDraft();
        OrderLine usd = new OrderLine(new ProductId(UUID.randomUUID()), Sku.of("X"),
            Money.of("1.00", "USD"), Quantity.of(1));

        assertThatThrownBy(() -> order.addLine(usd))
            .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    @DisplayName("cancellation reaches a terminal state and emits OrderCancelled")
    void cancellation() {
        Order order = newDraft();
        order.addLine(line("10.00", 1));
        order.confirm(clock);
        order.pullDomainEvents();

        order.cancel("customer changed mind", clock);

        assertThat(order.status().isTerminal()).isTrue();
        List<DomainEvent> events = order.pullDomainEvents();
        assertThat(events).singleElement().isInstanceOf(OrderEvent.OrderCancelled.class);
    }
}
