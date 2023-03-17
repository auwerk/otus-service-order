package org.auwerk.otus.arch.orderservice.service;

import java.util.UUID;

import org.auwerk.otus.arch.orderservice.domain.Order;

import io.smallrye.mutiny.Uni;

public interface OrderService {

    /**
     * Создание нового заказа
     * 
     * @return идентификатор созданного заказа
     */
    Uni<UUID> createOrder();

    /**
     * Размещение созданного заказа
     * 
     * @param orderId     идентификатор размещаемого заказа
     * @param productCode код заказываемого продукта
     * @param quantity    количество заказываемого продукта
     * @return размещенный заказ
     */
    Uni<Order> placeOrder(UUID orderId, String productCode, Integer quantity);
}
