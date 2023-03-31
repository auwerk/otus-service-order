package org.auwerk.otus.arch.orderservice.dao;

import java.util.List;
import java.util.UUID;

import org.auwerk.otus.arch.orderservice.domain.OrderStatusChange;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;

public interface OrderStatusChangeDao {

    public Uni<List<OrderStatusChange>> findAllByOrderId(PgPool pool, UUID orderId);

    public Uni<Void> insert(PgPool pool, UUID orderId, OrderStatusChange statusChange);
}
