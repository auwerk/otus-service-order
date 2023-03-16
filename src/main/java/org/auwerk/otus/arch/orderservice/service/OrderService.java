package org.auwerk.otus.arch.orderservice.service;

import java.util.UUID;

import org.auwerk.otus.arch.orderservice.domain.Order;

import io.smallrye.mutiny.Uni;

public interface OrderService {

    Uni<UUID> createOrder();

    Uni<Order> placeOrder(Order order);
    
}
