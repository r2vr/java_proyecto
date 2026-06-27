package com.orderflow.application.order.service;

import com.orderflow.application.order.port.in.ListOrdersUseCase;
import com.orderflow.application.order.port.out.OrderQueryPort;
import com.orderflow.application.order.query.OrderSummary;
import com.orderflow.application.order.query.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lists orders via the read-side query port. */
@Service
public class ListOrdersService implements ListOrdersUseCase {

    private final OrderQueryPort orderQueryPort;

    public ListOrdersService(OrderQueryPort orderQueryPort) {
        this.orderQueryPort = orderQueryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummary> handle(int page, int size, String status) {
        return orderQueryPort.list(page, size, status);
    }
}
