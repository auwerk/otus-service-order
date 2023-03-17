package org.auwerk.otus.arch.orderservice.dao.impl;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import org.auwerk.otus.arch.orderservice.dao.OrderDao;
import org.auwerk.otus.arch.orderservice.domain.Order;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;

@ApplicationScoped
public class OrderDaoImpl implements OrderDao {

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
                        "UPDATE orders SET product_code=$1, quantity=$2, placed_at=$3 WHERE id=$4")
                .execute(Tuple.of(order.getProductCode(), order.getQuantity(), order.getPlacedAt(), order.getId()))
                .map(rowSet -> rowSet.rowCount());
    }

    private Order mapRow(Row row) {
        return Order.builder()
                .id(row.getUUID("id"))
                .productCode(row.getString("product_code"))
                .quantity(row.getInteger("quantity"))
                .createdAt(row.getLocalDateTime("created_at"))
                .placedAt(row.getLocalDateTime("placed_at"))
                .build();
    }
}
