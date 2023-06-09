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
import org.auwerk.otus.arch.orderservice.exception.DaoException;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;

@ApplicationScoped
public class OrderDaoImpl implements OrderDao {

    @Override
    public Uni<List<Order>> findAllByUserName(PgPool pool, String userName, int pageSize, int page) {
        return pool.preparedQuery("SELECT * FROM orders WHERE username=$1 ORDER BY created_at DESC LIMIT $2 OFFSET $3")
                .execute(Tuple.of(userName, pageSize, pageSize * (page - 1)))
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
    public Uni<Void> insert(PgPool pool, UUID id, String userName, LocalDateTime createdAt) {
        return pool.preparedQuery("INSERT INTO orders(id, username, created_at, updated_at) VALUES($1, $2, $3, $4)")
                .execute(Tuple.of(id, userName, createdAt, createdAt))
                .flatMap(rowSet -> {
                    if (rowSet.rowCount() != 1) {
                        throw new DaoException("order insertion failed");
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    @Override
    public Uni<Void> updateStatus(PgPool pool, UUID id, OrderStatus status) {
        return pool.preparedQuery("UPDATE orders SET status=$1, updated_at=$2 WHERE id=$3")
                .execute(Tuple.of(status, LocalDateTime.now(), id))
                .flatMap(rowSet -> {
                    if (rowSet.rowCount() != 1) {
                        throw new DaoException("order status update failed");
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    private static Order mapRow(Row row) {
        return Order.builder()
                .id(row.getUUID("id"))
                .userName(row.getString("username"))
                .status(OrderStatus.valueOf(row.getString("status")))
                .createdAt(row.getLocalDateTime("created_at"))
                .updatedAt(row.getLocalDateTime("updated_at"))
                .build();
    }
}
