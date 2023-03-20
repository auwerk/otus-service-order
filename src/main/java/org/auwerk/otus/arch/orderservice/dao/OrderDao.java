package org.auwerk.otus.arch.orderservice.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.auwerk.otus.arch.orderservice.domain.Order;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;

public interface OrderDao {

    Uni<List<Order>> findAll(PgPool pool, int pageSize, int page);

    Uni<Order> findById(PgPool pool, UUID id);

    Uni<Integer> insert(PgPool pool, UUID id, LocalDateTime createdAt);

    Uni<Integer> update(PgPool pool, Order order);

}