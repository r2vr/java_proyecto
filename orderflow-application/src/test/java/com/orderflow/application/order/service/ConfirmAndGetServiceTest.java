package com.orderflow.application.order.service;

import com.orderflow.application.order.port.out.DomainEventPublisher;
import com.orderflow.application.order.query.OrderView;
import com.orderflow.domain.order.exception.InvalidOrderStateException;
import com.orderflow.domain.order.exception.OrderNotFoundException;
import com.orderflow.domain.order.model.*;
import com.orderflow.domain.order.port.OrderRepository;
import com.orderflow.domain.shared.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the confirm and get use cases using in-memory fakes — fast,
 * deterministic, and coupled only to the ports.
 */
@DisplayName("Confirm and Get use cases")
class ConfirmAndGetServiceTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-25T10:00:00Z"), ZoneOffset.UTC);

    private InMemoryOrderRepository repository;
    private ConfirmOrderService confirm;
    private GetOrderService get;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
        confirm = new ConfirmOrderService(repository, new NoopPublisher(), clock);
        get = new GetOrderService(repository);
    }

    private OrderId seedDraftWithLine() {
        Order order = Order.create(OrderId.newId(), new CustomerId(UUID.randomUUID()), EUR, clock);
        order.addLine(new OrderLine(new ProductId(UUID.randomUUID()), Sku.of("SKU-1"),
            Money.of("10.00", "EUR"), Quantity.of(1)));
        order.pullDomainEvents();
        repository.save(order);
        return order.id();
    }

    @Test
    @DisplayName("confirm moves a draft to CONFIRMED")
    void confirmsDraft() {
        OrderId id = seedDraftWithLine();

        OrderView view = confirm.handle(id);

        assertThat(view.status()).isEqualTo("CONFIRMED");
        assertThat(repository.findById(id).orElseThrow().status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("confirm fails for an unknown order")
    void confirmUnknown() {
        assertThatThrownBy(() -> confirm.handle(OrderId.newId()))
            .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("confirming twice is rejected by the state machine")
    void confirmTwice() {
        OrderId id = seedDraftWithLine();
        confirm.handle(id);

        assertThatThrownBy(() -> confirm.handle(id))
            .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    @DisplayName("get returns the view or 404")
    void getOrThrow() {
        OrderId id = seedDraftWithLine();
        assertThat(get.handle(id).orderId()).isEqualTo(id.toString());

        assertThatThrownBy(() -> get.handle(OrderId.newId()))
            .isInstanceOf(OrderNotFoundException.class);
    }

    // --- fakes ---
    private static final class InMemoryOrderRepository implements OrderRepository {
        private final Map<OrderId, Order> store = new HashMap<>();
        public void save(Order order) { store.put(order.id(), order); }
        public Optional<Order> findById(OrderId id) { return Optional.ofNullable(store.get(id)); }
    }

    private static final class NoopPublisher implements DomainEventPublisher {
        public void publish(List<DomainEvent> events) { /* no-op */ }
    }
}
