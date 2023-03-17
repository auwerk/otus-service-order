package org.auwerk.otus.arch.orderservice.service.impl;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import org.auwerk.otus.arch.orderservice.dao.OrderDao;
import org.auwerk.otus.arch.orderservice.domain.Order;
import org.auwerk.otus.arch.orderservice.exception.OrderAlreadyPlacedException;
import org.auwerk.otus.arch.orderservice.exception.OrderNotFoundException;
import org.auwerk.otus.arch.orderservice.service.OrderService;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final PgPool pool;
    private final OrderDao dao;

    @Override
    public Uni<UUID> createOrder() {
        return dao.insert(pool);
    }

    @Override
    public Uni<Order> placeOrder(UUID orderId, String productCode, Integer quantity) {
        return pool.withTransaction(conn -> dao.findById(pool, orderId)
                .onItem()
                .invoke(order -> {
                    if (order.getPlacedAt() != null) {
                        throw new OrderAlreadyPlacedException(order.getId());
                    }
                    order.setPlacedAt(LocalDateTime.now());
                    order.setProductCode(productCode);
                    order.setQuantity(quantity);
                })
                .call(order -> dao.update(pool, order)))
                .onFailure(NoSuchElementException.class)
                .transform(ex -> new OrderNotFoundException(orderId));
    }
}
