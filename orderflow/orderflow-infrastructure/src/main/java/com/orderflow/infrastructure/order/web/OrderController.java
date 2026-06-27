package com.orderflow.infrastructure.order.web;

import com.orderflow.application.order.port.in.ConfirmOrderUseCase;
import com.orderflow.application.order.port.in.CreateOrderCommand;
import com.orderflow.application.order.port.in.CreateOrderUseCase;
import com.orderflow.application.order.port.in.GetOrderUseCase;
import com.orderflow.application.order.port.in.ListOrdersUseCase;
import com.orderflow.application.order.query.OrderSummary;
import com.orderflow.application.order.query.OrderView;
import com.orderflow.application.order.query.Page;
import com.orderflow.domain.order.model.OrderId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * REST adapter (driving side). It only translates HTTP to the input ports and
 * back; all business logic lives behind the use cases. Thin controllers like this
 * keep the delivery mechanism replaceable.
 * <p>
 * A malformed id in the path produces an {@link IllegalArgumentException} from
 * {@code OrderId.of}, which the {@link GlobalExceptionHandler} turns into 400.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final CreateOrderUseCase createOrder;
    private final ConfirmOrderUseCase confirmOrder;
    private final GetOrderUseCase getOrder;
    private final ListOrdersUseCase listOrders;

    public OrderController(CreateOrderUseCase createOrder,
                           ConfirmOrderUseCase confirmOrder,
                           GetOrderUseCase getOrder,
                           ListOrdersUseCase listOrders) {
        this.createOrder = createOrder;
        this.confirmOrder = confirmOrder;
        this.getOrder = getOrder;
        this.listOrders = listOrders;
    }

    @PostMapping
    public ResponseEntity<CreateOrderResponse> create(@Valid @RequestBody CreateOrderRequest request,
                                                      UriComponentsBuilder uriBuilder) {
        CreateOrderCommand command = new CreateOrderCommand(
            request.customerId(),
            request.currency(),
            request.lines().stream()
                .map(l -> new CreateOrderCommand.Line(l.productId(), l.sku(), l.unitPrice(), l.quantity()))
                .toList());

        OrderId id = createOrder.handle(command);

        URI location = uriBuilder.path("/api/orders/{id}").buildAndExpand(id.value()).toUri();
        return ResponseEntity.created(location).body(new CreateOrderResponse(id.toString()));
    }

    @PostMapping("/{id}/confirm")
    public OrderResponse confirm(@PathVariable String id) {
        OrderView view = confirmOrder.handle(OrderId.of(id));
        return OrderResponse.from(view);
    }

    @GetMapping
    public Page<OrderSummary> list(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(required = false) String status) {
        return listOrders.handle(page, size, status);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable String id) {
        OrderView view = getOrder.handle(OrderId.of(id));
        return OrderResponse.from(view);
    }
}
