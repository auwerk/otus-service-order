package org.auwerk.otus.arch.orderservice.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Function;

import org.auwerk.otus.arch.orderservice.dao.OrderDao;
import org.auwerk.otus.arch.orderservice.dao.OrderPositionDao;
import org.auwerk.otus.arch.orderservice.domain.Order;
import org.auwerk.otus.arch.orderservice.domain.OrderPosition;
import org.auwerk.otus.arch.orderservice.domain.OrderStatus;
import org.auwerk.otus.arch.orderservice.exception.OrderAlreadyPlacedException;
import org.auwerk.otus.arch.orderservice.exception.OrderCreatedByDifferentUserException;
import org.auwerk.otus.arch.orderservice.exception.OrderNotFoundException;
import org.auwerk.otus.arch.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.SqlConnection;

public class OrderServiceImplTest {

    private static final String USERNAME = "user";

    private static final String PRODUCT_CODE = "PRODUCT1";
    private static final int QUANTITY = 16;

    private final PgPool pool = mock(PgPool.class);
    private final OrderDao orderDao = mock(OrderDao.class);
    private final OrderPositionDao positionDao = mock(OrderPositionDao.class);
    private final SecurityIdentity securityIdentity = mock(SecurityIdentity.class);
    private final OrderService service = new OrderServiceImpl(pool, orderDao, positionDao, securityIdentity);

    @BeforeEach
    void mockTransaction() {
        when(pool.withTransaction(any()))
        .then(inv -> {
            final Function<SqlConnection, Uni<Order>> f = inv.getArgument(0);
            return f.apply(null);
        });
    }

    @BeforeEach
    void mockUser() {
        var principal = mock(Principal.class);
        when(principal.getName()).thenReturn(USERNAME);
        when(securityIdentity.getPrincipal()).thenReturn(principal);
    }

    @Test
    void listOrders_success() {
        // given
        final var pageSize = 10;
        final var page = 1;

        // when
        when(orderDao.findAllByUserName(eq(pool), anyString(), anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(List.of(
                    Order.builder().build(),
                    Order.builder().build(),
                    Order.builder().build()
                )));
        final var subscriber = service.findAllOrders(pageSize, page).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        final var orders = subscriber.assertCompleted().getItem();
        assertNotNull(orders);
        assertEquals(3, orders.size());

        verify(orderDao, times(1))
                .findAllByUserName(eq(pool), eq(USERNAME), eq(pageSize), eq(page));
    }

    @Test
    void createOrder_success() {
        // when
        when(orderDao.insert(eq(pool), any(UUID.class), anyString(), any(LocalDateTime.class)))
                .thenReturn(Uni.createFrom().item(1));
        final var subscriber = service.createOrder().subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        subscriber.assertCompleted();

        verify(orderDao, times(1))
                .insert(eq(pool), any(UUID.class), eq(USERNAME), any(LocalDateTime.class));
    }

    @Test
    void createOrder_insertFailure() {
        // given
        final var errorMessage = "failed to insert order";

        // when
        when(orderDao.insert(eq(pool), any(UUID.class), anyString(), any(LocalDateTime.class)))
                .thenReturn(Uni.createFrom().item(0));
        final var subscriber = service.createOrder().subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        final var failure = subscriber.assertFailedWith(RuntimeException.class)
                .getFailure();
        assertEquals(errorMessage, failure.getMessage());
    }

    @Test
    void placeOrder_success() {
        // given
        final var orderId = UUID.randomUUID();
        final var orderPositions = List.of(OrderPosition.builder()
                .productCode(PRODUCT_CODE)
                .quantity(QUANTITY)
                .build());

        // when
        when(orderDao.findById(pool, orderId))
                .thenReturn(Uni.createFrom().item(Order.builder()
                        .id(orderId)
                        .status(OrderStatus.CREATED)
                        .createdAt(LocalDateTime.now())
                        .userName(USERNAME)
                        .build()));
        final var subscriber = service.placeOrder(orderId, orderPositions).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        final var placedOrder = subscriber.assertCompleted().getItem();
        assertNotNull(placedOrder);
        assertEquals(orderId, placedOrder.getId());
        assertNotNull(placedOrder.getCreatedAt());
        assertNotNull(placedOrder.getPlacedAt());
        final var placedPositions = placedOrder.getPositions();
        assertNotNull(placedPositions);
        assertEquals(1, placedPositions.size());
        assertEquals(PRODUCT_CODE, placedPositions.get(0).getProductCode());
        assertEquals(QUANTITY, placedPositions.get(0).getQuantity());

        verify(positionDao, times(1))
                .insert(eq(pool), eq(orderId), any(OrderPosition.class));

        verify(orderDao, times(1))
                .update(eq(pool), any(Order.class));
    }

    @Test
    void placeOrder_notFound() {
        // given
        final var orderId = UUID.randomUUID();
        final var orderPositions = List.of(OrderPosition.builder()
                .productCode(PRODUCT_CODE)
                .quantity(QUANTITY)
                .build());

        // when
        when(orderDao.findById(eq(pool), any(UUID.class)))
                .thenReturn(Uni.createFrom().failure(new NoSuchElementException("order not found")));
        final var subscriber = service.placeOrder(orderId, orderPositions).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        final OrderNotFoundException failure = (OrderNotFoundException) subscriber
                .assertFailedWith(OrderNotFoundException.class)
                .getFailure();
        assertEquals(orderId, failure.getOrderId());
    }

    @Test
    void placeOrder_createdByDifferentUser() {
        // given
        final var orderId = UUID.randomUUID();
        final var orderPositions = List.of(OrderPosition.builder()
                .productCode(PRODUCT_CODE)
                .quantity(QUANTITY)
                .build());

        // when
        when(orderDao.findById(eq(pool), eq(orderId)))
                .thenReturn(Uni.createFrom().item(Order.builder()
                        .id(orderId)
                        .createdAt(LocalDateTime.now())
                        .status(OrderStatus.CREATED)
                        .userName("other-user")
                        .build()));
        final var subscriber = service.placeOrder(orderId, orderPositions).subscribe()
                .withSubscriber(UniAssertSubscriber.create());
        // then
        final OrderCreatedByDifferentUserException failure = (OrderCreatedByDifferentUserException) subscriber
                .assertFailedWith(OrderCreatedByDifferentUserException.class)
                .getFailure();
        assertEquals(orderId, failure.getOrderId());
    }

    @Test
    void placeOrder_alreadyPlaced() {
        // given
        final var orderId = UUID.randomUUID();
        final var orderPositions = List.of(OrderPosition.builder()
                .productCode(PRODUCT_CODE)
                .quantity(QUANTITY)
                .build());

        // when
        when(orderDao.findById(eq(pool), eq(orderId)))
                .thenReturn(Uni.createFrom().item(Order.builder()
                        .id(orderId)
                        .createdAt(LocalDateTime.now())
                        .status(OrderStatus.PLACED)
                        .userName(USERNAME)
                        .placedAt(LocalDateTime.now())
                        .build()));
        final var subscriber = service.placeOrder(orderId, orderPositions).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        final OrderAlreadyPlacedException failure = (OrderAlreadyPlacedException) subscriber
                .assertFailedWith(OrderAlreadyPlacedException.class)
                .getFailure();
        assertEquals(orderId, failure.getOrderId());
    }
}
