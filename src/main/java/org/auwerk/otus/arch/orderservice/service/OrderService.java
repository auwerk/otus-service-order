package org.auwerk.otus.arch.orderservice.service;

import java.util.List;
import java.util.UUID;

import org.auwerk.otus.arch.orderservice.domain.Order;

import io.smallrye.mutiny.Uni;

public interface OrderService {

    Uni<List<Order>> getAllOrders(int pageSize, int page);

    Uni<Order> getOrderById(UUID id);

    /**
     * Создание нового заказа
     * 
     * @return идентификатор созданного заказа
     */
    Uni<UUID> createOrder();

    /**
     * Добавление позиции в заказ
     * 
     * @return идентификатор добавленной позиции
     */
    Uni<UUID> addOrderPosition(UUID orderId, String productCode, Integer quantity);

    /**
     * Удаление позиции из заказа
     * 
     * @param positionId идентификатор удаляемой позиции
     * @return
     */
    Uni<Void> removeOrderPosition(UUID positionId);

    /**
     * Размещение созданного заказа
     * 
     * @param orderId идентификатор размещаемого заказа
     * @return
     */
    Uni<Void> placeOrder(UUID orderId);

    /**
     * Отмена созданного заказа
     * 
     * @param orderId идентификатор отменяемого заказа
     * @return
     */
    Uni<Void> cancelOrder(UUID orderId);
}
