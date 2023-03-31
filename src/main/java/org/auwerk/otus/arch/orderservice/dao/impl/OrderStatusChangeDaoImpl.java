package org.auwerk.otus.arch.orderservice.dao.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import org.auwerk.otus.arch.orderservice.dao.OrderStatusChangeDao;
import org.auwerk.otus.arch.orderservice.domain.OrderStatus;
import org.auwerk.otus.arch.orderservice.domain.OrderStatusChange;
import org.auwerk.otus.arch.orderservice.exception.DaoException;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;

@ApplicationScoped
public class OrderStatusChangeDaoImpl implements OrderStatusChangeDao {

    @Override
    public Uni<List<OrderStatusChange>> findAllByOrderId(PgPool pool, UUID orderId) {
        return pool.preparedQuery("SELECT * FROM order_status_changes WHERE order_id=$1 ORDER BY created_at DESC")
                .execute(Tuple.of(orderId))
                .map(rowSet -> {
                    final var result = new ArrayList<OrderStatusChange>(rowSet.rowCount());
                    final var rowSetIterator = rowSet.iterator();
                    while (rowSetIterator.hasNext()) {
                        result.add(mapRow(rowSetIterator.next()));
                    }
                    return result;
                });
    }

    @Override
    public Uni<Void> insert(PgPool pool, UUID orderId, OrderStatusChange statusChange) {
        return pool
                .preparedQuery(
                        "INSERT INTO order_status_changes(id, order_id, status, created_at) VALUES($1, $2, $3, $4)")
                .execute(Tuple.of(UUID.randomUUID(), orderId, statusChange.getStatus().name(),
                        statusChange.getCreatedAt()))
                .flatMap(rowSet -> {
                    if (rowSet.rowCount() != 1) {
                        throw new DaoException("insertion failed");
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    private static OrderStatusChange mapRow(Row row) {
        return OrderStatusChange.builder()
                .status(OrderStatus.valueOf(row.getString("status")))
                .createdAt(row.getLocalDateTime("created_at"))
                .build();
    }
}
