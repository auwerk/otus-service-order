package org.auwerk.otus.arch.orderservice.dao;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.auwerk.otus.arch.orderservice.domain.OrderPosition;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;

public interface OrderPositionDao {

    Uni<OrderPosition> findById(PgPool pool, UUID id);

    Uni<List<OrderPosition>> findAllByOrderId(PgPool pool, UUID orderId);

    Uni<UUID> insert(PgPool pool, UUID orderId, OrderPosition position);

    Uni<Void> updatePriceById(PgPool pool, UUID id, BigDecimal price);

    Uni<Void> deleteById(PgPool pool, UUID id);
}
