package org.auwerk.otus.arch.orderservice.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Function;

import org.auwerk.otus.arch.orderservice.dao.OrderDao;
import org.auwerk.otus.arch.orderservice.domain.Order;
import org.auwerk.otus.arch.orderservice.exception.OrderAlreadyPlacedException;
import org.auwerk.otus.arch.orderservice.exception.OrderNotFoundException;
import org.auwerk.otus.arch.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.SqlConnection;

public class OrderServiceImplTest {

    private static final String PRODUCT_CODE = "PRODUCT1";
    private static final int QUANTITY = 16;

    private final PgPool pool = mock(PgPool.class);
    private final OrderDao dao = mock(OrderDao.class);
    private final OrderService service = new OrderServiceImpl(pool, dao);

    @BeforeEach
    void mockTransaction() {
        when(pool.withTransaction(any()))
        .then(inv -> {
            final Function<SqlConnection, Uni<Order>> f = inv.getArgument(0);
            return f.apply(null);
        });
    }

    @Test
    void createOrder_success() {
        // given
        final var orderId = UUID.randomUUID();

        // when
        when(dao.insert(pool)).thenReturn(Uni.createFrom().item(orderId));
        final var subscriber = service.createOrder().subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        subscriber.assertItem(orderId);
    }

    @Test
    void createOrder_insertFailure() {
        // given
        final var errorMessage = "error inserting order";

        // when
        when(dao.insert(pool))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException(errorMessage)));
        final var subscriber = service.createOrder().subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        final var failure = subscriber.assertFailedWith(IllegalStateException.class)
                .getFailure();
        assertEquals(errorMessage, failure.getMessage());
    }

    @Test
    void placeOrder_success() {
        // given
        final var orderId = UUID.randomUUID();

        // when
        when(dao.findById(pool, orderId))
                .thenReturn(Uni.createFrom().item(Order.builder()
                        .id(orderId)
                        .createdAt(LocalDateTime.now())
                        .build()));
        final var subscriber = service.placeOrder(orderId, PRODUCT_CODE, QUANTITY).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        final var placedOrder = subscriber.assertCompleted().getItem();
        assertNotNull(placedOrder);
        assertEquals(orderId, placedOrder.getId());
        assertEquals(PRODUCT_CODE, placedOrder.getProductCode());
        assertEquals(QUANTITY, placedOrder.getQuantity());
        assertNotNull(placedOrder.getCreatedAt());
        assertNotNull(placedOrder.getPlacedAt());

        verify(dao, times(1))
                .update(eq(pool), any(Order.class));
    }

    @Test
    void placeOrder_notFound() {
        // given
        final var orderId = UUID.randomUUID();

        // when
        when(dao.findById(eq(pool), any(UUID.class)))
                .thenReturn(Uni.createFrom().failure(new NoSuchElementException("order not found")));
        final var subscriber = service.placeOrder(orderId, PRODUCT_CODE, QUANTITY).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        final OrderNotFoundException failure = (OrderNotFoundException) subscriber
                .assertFailedWith(OrderNotFoundException.class)
                .getFailure();
        assertEquals(orderId, failure.getOrderId());
    }

    @Test
    void placeOrder_alreadyPlaced() {
        // given
        final var orderId = UUID.randomUUID();

        // when
        when(dao.findById(eq(pool), eq(orderId)))
                .thenReturn(Uni.createFrom().item(Order.builder()
                        .id(orderId)
                        .createdAt(LocalDateTime.now())
                        .productCode(PRODUCT_CODE)
                        .quantity(QUANTITY)
                        .placedAt(LocalDateTime.now())
                        .build()));
        final var subscriber = service.placeOrder(orderId, PRODUCT_CODE, QUANTITY).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        final OrderAlreadyPlacedException failure = (OrderAlreadyPlacedException) subscriber
                .assertFailedWith(OrderAlreadyPlacedException.class)
                .getFailure();
        assertEquals(orderId, failure.getOrderId());
    }
}
