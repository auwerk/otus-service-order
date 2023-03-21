package org.auwerk.otus.arch.orderservice.dao;

import java.util.List;
import java.util.UUID;

import org.auwerk.otus.arch.orderservice.domain.OrderPosition;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;

public interface OrderPositionDao {

    public Uni<List<OrderPosition>> findAllByOrderId(PgPool pool, UUID orderId);

    public Uni<Integer> insert(PgPool pool, UUID orderId, OrderPosition position);
}
