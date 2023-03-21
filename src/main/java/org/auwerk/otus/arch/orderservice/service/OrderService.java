package org.auwerk.otus.arch.orderservice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.auwerk.otus.arch.orderservice.domain.Order;
import org.auwerk.otus.arch.orderservice.domain.OrderPosition;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;

public interface OrderService {

    Uni<List<Order>> findAllOrders(int pageSize, int page);

    /**
     * Создание нового заказа
     * 
     * @return идентификатор созданного заказа и время его создания
     */
    Uni<Tuple2<UUID, LocalDateTime>> createOrder();

    /**
     * Размещение созданного заказа
     * 
     * @param orderId   идентификатор размещаемого заказа
     * @param positions позиции заказа
     * @return размещенный заказ
     */
    Uni<Order> placeOrder(UUID orderId, List<OrderPosition> positions);
}
