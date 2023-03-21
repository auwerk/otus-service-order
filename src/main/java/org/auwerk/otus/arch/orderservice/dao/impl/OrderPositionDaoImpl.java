package org.auwerk.otus.arch.orderservice.dao.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import org.auwerk.otus.arch.orderservice.dao.OrderPositionDao;
import org.auwerk.otus.arch.orderservice.domain.OrderPosition;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;

@ApplicationScoped
public class OrderPositionDaoImpl implements OrderPositionDao {

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
    public Uni<Integer> insert(PgPool pool, UUID orderId, OrderPosition position) {
        return pool
                .preparedQuery(
                        "INSERT INTO order_positions(id, order_id, product_code, quantity) VALUES($1, $2, $3, $4)")
                .execute(Tuple.of(UUID.randomUUID(), orderId, position.getProductCode(), position.getQuantity()))
                .map(rowSet -> rowSet.rowCount());
    }

    private static OrderPosition mapRow(Row row) {
        return OrderPosition.builder()
                .productCode(row.getString("product_code"))
                .quantity(row.getInteger("quantity"))
                .build();
    }
}
