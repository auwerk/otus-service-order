package org.auwerk.otus.arch.orderservice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.auwerk.otus.arch.orderservice.domain.Order;

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
     * @param orderId     идентификатор размещаемого заказа
     * @param productCode код заказываемого продукта
     * @param quantity    количество заказываемого продукта
     * @return размещенный заказ
     */
    Uni<Order> placeOrder(UUID orderId, String productCode, Integer quantity);
}
