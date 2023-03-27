package org.auwerk.otus.arch.orderservice.dao;

import java.util.List;
import java.util.UUID;

import org.auwerk.otus.arch.orderservice.domain.OrderPosition;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;

public interface OrderPositionDao {

    public Uni<OrderPosition> findById(PgPool pool, UUID id);

    public Uni<List<OrderPosition>> findAllByOrderId(PgPool pool, UUID orderId);

    public Uni<UUID> insert(PgPool pool, UUID orderId, OrderPosition position);

    public Uni<Void> deleteById(PgPool pool, UUID id);
}
