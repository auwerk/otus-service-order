package org.auwerk.otus.arch.orderservice.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.auwerk.otus.arch.orderservice.domain.Order;
import org.auwerk.otus.arch.orderservice.domain.OrderStatus;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;

public interface OrderDao {

    Uni<List<Order>> findAllByUserName(PgPool pool, String userName, int pageSize, int page);

    Uni<Order> findById(PgPool pool, UUID id);

    Uni<Void> insert(PgPool pool, UUID id, String userName, LocalDateTime createdAt);

    Uni<Void> updateStatus(PgPool pool, UUID id, OrderStatus status);
}