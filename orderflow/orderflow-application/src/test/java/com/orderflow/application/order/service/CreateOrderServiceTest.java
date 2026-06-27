package com.orderflow.application.order.service;

import com.orderflow.application.order.port.in.CreateOrderCommand;
import com.orderflow.domain.order.event.OrderEvent;
import com.orderflow.domain.order.model.Money;
import com.orderflow.domain.order.model.Order;
import com.orderflow.domain.order.model.OrderId;
import com.orderflow.domain.order.model.OrderStatus;
import com.orderflow.domain.order.port.OrderRepository;
import com.orderflow.domain.shared.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for the create-order use case. Uses hand-rolled fakes instead of a
 * mocking framework: the test stays fast, readable and coupled only to the ports'
 * contracts. A fixed {@link Clock} keeps it deterministic.
 */
@DisplayName("CreateOrderService")
class CreateOrderServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-25T10:00:00Z"), ZoneOffset.UTC);
    private InMemoryOrderRepository repository;
    private CapturingPublisher publisher;
    private CreateOrderService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
        publisher = new CapturingPublisher();
        service = new CreateOrderService(repository, publisher, clock);
    }

    @Test
    @DisplayName("persists a draft order and publishes the creation event")
    void createsOrder() {
        CreateOrderCommand command = new CreateOrderCommand(
            UUID.randomUUID().toString(),
            "EUR",
            List.of(new CreateOrderCommand.Line(
                UUID.randomUUID().toString(), "SKU-1", new BigDecimal("19.99"), 2)));

        OrderId id = service.handle(command);

        Order saved = repository.findById(id).orElseThrow();
        assertThat(saved.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(saved.total()).isEqualTo(Money.of("39.98", "EUR"));
        assertThat(publisher.events)
            .singleElement()
            .isInstanceOf(OrderEvent.OrderCreated.class);
    }

    // --- fakes -------------------------------------------------------------

    private static final class InMemoryOrderRepository implements OrderRepository {
        private final Map<OrderId, Order> store = new HashMap<>();

        @Override
        public void save(Order order) {
            store.put(order.id(), order);
        }

        @Override
        public Optional<Order> findById(OrderId id) {
            return Optional.ofNullable(store.get(id));
        }
    }

    private static final class CapturingPublisher
            implements com.orderflow.application.order.port.out.DomainEventPublisher {
        private final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void publish(List<DomainEvent> events) {
            this.events.addAll(events);
        }
    }
}
