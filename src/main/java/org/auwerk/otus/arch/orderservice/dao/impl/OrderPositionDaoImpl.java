package org.auwerk.otus.arch.orderservice.dao.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import org.auwerk.otus.arch.orderservice.dao.OrderPositionDao;
import org.auwerk.otus.arch.orderservice.domain.OrderPosition;
import org.auwerk.otus.arch.orderservice.exception.DaoException;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;

@ApplicationScoped
public class OrderPositionDaoImpl implements OrderPositionDao {

    @Override
    public Uni<OrderPosition> findById(PgPool pool, UUID id) {
        return pool.preparedQuery("SELECT * FROM order_positions WHERE id=$1")
                .execute(Tuple.of(id))
                .map(rowSet -> {
                    final var rowSetIterator = rowSet.iterator();
                    if (!rowSetIterator.hasNext()) {
                        throw new NoSuchElementException("order position not found, id=" + id);
                    }
                    return mapRow(rowSetIterator.next());
                });
    }

    @Override
    public Uni<List<OrderPosition>> findAllByOrderId(PgPool pool, UUID orderId) {
        return pool.preparedQuery("SELECT * FROM order_positions WHERE order_id=$1")
                .execute(Tuple.of(orderId))
                .map(rowSet -> {
                    final var result = new ArrayList<OrderPosition>(rowSet.rowCount());
                    final var rowSetIterator = rowSet.iterator();
                    while (rowSetIterator.hasNext()) {
                        result.add(mapRow(rowSetIterator.next()));
                    }
                    return result;
                });
    }

    @Override
    public Uni<UUID> insert(PgPool pool, UUID orderId, OrderPosition position) {
        return pool
                .preparedQuery(
                        "INSERT INTO order_positions(id, order_id, product_code, quantity) VALUES($1, $2, $3, $4) RETURNING id")
                .execute(Tuple.of(UUID.randomUUID(), orderId, position.getProductCode(), position.getQuantity()))
                .map(rowSet -> {
                    if (rowSet.rowCount() != 1) {
                        throw new DaoException("order position insertion failed");
                    }
                    return rowSet.iterator().next().getUUID("id");
                });
    }

    @Override
    public Uni<Void> deleteById(PgPool pool, UUID id) {
        return pool.preparedQuery("DELETE FROM order_positions WHERE id=$1")
                .execute(Tuple.of(id))
                .flatMap(rowSet -> {
                    if (rowSet.rowCount() != 1) {
                        throw new DaoException("order position deletion failed");
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    private static OrderPosition mapRow(Row row) {
        return OrderPosition.builder()
                .id(row.getUUID("id"))
                .productCode(row.getString("product_code"))
                .quantity(row.getInteger("quantity"))
                .price(row.getBigDecimal("price"))
                .build();
    }
}
