package org.auwerk.otus.arch.orderservice.dao.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import org.auwerk.otus.arch.orderservice.dao.OrderDao;
import org.auwerk.otus.arch.orderservice.domain.Order;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class OrderDaoImpl implements OrderDao {

    private final PgPool client;

    @Override
    public Uni<UUID> insertOrder() {
        return client.preparedQuery("INSERT INTO orders(id, created_at) VALUES($1, $2) RETURNING id")
                .execute(Tuple.of(UUID.randomUUID(), LocalDateTime.now()))
                .map(rowSet -> rowSet.iterator().next().getUUID("id"));
    }

    @Override
    public Uni<Integer> updateOrder(Order order) {
        return client
                .preparedQuery(
                        "UPDATE orders SET product_code=$1, quantity=$2, placed_at=$3 WHERE id=$4")
                .execute(Tuple.of(order.getProductCode(), order.getQuantity(), order.getPlacedAt()))
                .map(rowSet -> rowSet.rowCount());
    }
}
