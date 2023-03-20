package org.auwerk.otus.arch.orderservice.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import org.auwerk.otus.arch.orderservice.dao.OrderDao;
import org.auwerk.otus.arch.orderservice.domain.Order;
import org.auwerk.otus.arch.orderservice.domain.OrderStatus;
import org.auwerk.otus.arch.orderservice.exception.OrderAlreadyPlacedException;
import org.auwerk.otus.arch.orderservice.exception.OrderNotFoundException;
import org.auwerk.otus.arch.orderservice.service.OrderService;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.vertx.mutiny.pgclient.PgPool;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final PgPool pool;
    private final OrderDao dao;

    @Override
    public Uni<List<Order>> findAllOrders(int pageSize, int page) {
        return dao.findAll(pool, pageSize, page);
    }

    @Override
    public Uni<Tuple2<UUID, LocalDateTime>> createOrder() {
        final var id = UUID.randomUUID();
        final var createdAt = LocalDateTime.now();
        return dao.insert(pool, id, createdAt).map(insertedRows -> {
            if (insertedRows < 1) {
                throw new RuntimeException("failed to insert order");
            }
            return Tuple2.of(id, createdAt);
        });
    }

    @Override
    public Uni<Order> placeOrder(UUID orderId, String productCode, Integer quantity) {
        return pool.withTransaction(conn -> dao.findById(pool, orderId)
                .onItem()
                .invoke(order -> {
                    if (OrderStatus.PLACED.equals(order.getStatus())) {
                        throw new OrderAlreadyPlacedException(order.getId());
                    }
                    order.setStatus(OrderStatus.PLACED);
                    order.setPlacedAt(LocalDateTime.now());
                    order.setProductCode(productCode);
                    order.setQuantity(quantity);
                })
                .call(order -> dao.update(pool, order)))
                .onFailure(NoSuchElementException.class)
                .transform(ex -> new OrderNotFoundException(orderId));
    }
}
