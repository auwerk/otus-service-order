package org.auwerk.otus.arch.orderservice.dao.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import org.auwerk.otus.arch.orderservice.dao.OrderDao;
import org.auwerk.otus.arch.orderservice.domain.Order;
import org.auwerk.otus.arch.orderservice.domain.OrderStatus;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;

@ApplicationScoped
public class OrderDaoImpl implements OrderDao {

    @Override
    public Uni<List<Order>> findAll(PgPool pool, int pageSize, int page) {
        return pool.preparedQuery("SELECT * FROM orders ORDER BY created_at DESC LIMIT $1 OFFSET $2")
                .execute(Tuple.of(pageSize, pageSize * (page - 1)))
                .map(rowSet -> {
                    final var result = new ArrayList<Order>(pageSize);
                    final var rowSetIterator = rowSet.iterator();
                    while (rowSetIterator.hasNext()) {
                        result.add(mapRow(rowSetIterator.next()));
                    }
                    return result;
                });
    }

    @Override
    public Uni<Order> findById(PgPool pool, UUID id) {
        return pool.preparedQuery("SELECT * FROM orders WHERE id=$1")
                .execute(Tuple.of(id))
                .map(rowSet -> {
                    final var rowSetIterator = rowSet.iterator();
                    if (!rowSetIterator.hasNext()) {
                        throw new NoSuchElementException("order not found, id=" + id);
                    }
                    return mapRow(rowSetIterator.next());
                });
    }

    @Override
    public Uni<Integer> insert(PgPool pool, UUID id, LocalDateTime createdAt) {
        return pool.preparedQuery("INSERT INTO orders(id, created_at) VALUES($1, $2)")
                .execute(Tuple.of(id, createdAt))
                .map(rowSet -> rowSet.rowCount());
    }

    @Override
    public Uni<Integer> update(PgPool pool, Order order) {
        return pool
                .preparedQuery(
                        "UPDATE orders SET status=$1, placed_at=$2 WHERE id=$3")
                .execute(Tuple.of(order.getStatus().name(), order.getPlacedAt(), order.getId()))
                .map(rowSet -> rowSet.rowCount());
    }

    private static Order mapRow(Row row) {
        return Order.builder()
                .id(row.getUUID("id"))
                .status(OrderStatus.valueOf(row.getString("status")))
                .createdAt(row.getLocalDateTime("created_at"))
                .placedAt(row.getLocalDateTime("placed_at"))
                .build();
    }
}
