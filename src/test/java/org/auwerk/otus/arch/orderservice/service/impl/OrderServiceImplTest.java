package org.auwerk.otus.arch.orderservice.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;

import org.auwerk.otus.arch.orderservice.dao.OrderDao;
import org.auwerk.otus.arch.orderservice.domain.Order;
import org.auwerk.otus.arch.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

public class OrderServiceImplTest {

    private final OrderDao dao = mock(OrderDao.class);
    private final OrderService service = new OrderServiceImpl(dao);

    @Test
    void createOrder_success() {
        // given
        final var orderId = UUID.randomUUID();

        // when
        when(dao.insertOrder()).thenReturn(Uni.createFrom().item(orderId));
        final var subscriber = service.createOrder().subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        subscriber
                .awaitItem()
                .assertItem(orderId);
    }

    @Test
    void placeOrder_success() {
        // given
        final var order = Order.builder()
                .id(UUID.randomUUID())
                .productCode("PRODUCT1")
                .quantity(6)
                .createdAt(LocalDateTime.now())
                .build();

        // when
        final var subscriber = service.placeOrder(order).subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        final var placedOrder = subscriber.assertCompleted().getItem();
        assertNotNull(placedOrder);
        assertNotNull(placedOrder.getPlacedAt());
        assertTrue(placedOrder.getPlacedAt().isEqual(order.getCreatedAt())
                || placedOrder.getPlacedAt().isAfter(order.getCreatedAt()));
    }
}
