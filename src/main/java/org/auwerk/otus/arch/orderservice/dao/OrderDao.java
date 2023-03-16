package org.auwerk.otus.arch.orderservice.dao;

import java.util.UUID;

import org.auwerk.otus.arch.orderservice.domain.Order;

import io.smallrye.mutiny.Uni;

public interface OrderDao {

    Uni<UUID> insertOrder();

    Uni<Integer> updateOrder(Order order);

}
