package org.auwerk.otus.arch.orderservice.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import org.auwerk.otus.arch.orderservice.dao.OrderStatusChangeDao;
import org.auwerk.otus.arch.orderservice.domain.Order;
import org.auwerk.otus.arch.orderservice.domain.OrderPosition;
import org.auwerk.otus.arch.orderservice.domain.OrderStatus;
import org.auwerk.otus.arch.orderservice.exception.OrderAlreadyPlacedException;
import org.auwerk.otus.arch.orderservice.exception.OrderCanNotBeCanceledException;
import org.auwerk.otus.arch.orderservice.exception.OrderCreatedByDifferentUserException;
import org.auwerk.otus.arch.orderservice.exception.OrderNotFoundException;
import org.auwerk.otus.arch.orderservice.exception.OrderPositionNotFoundException;
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

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID POSITION_ID = UUID.randomUUID();
    private static final String PRODUCT_CODE = "PRODUCT1";
    private static final int QUANTITY = 16;

    private final PgPool pool = mock(PgPool.class);
    private final OrderDao orderDao = mock(OrderDao.class);
    private final OrderPositionDao positionDao = mock(OrderPositionDao.class);
    private final OrderStatusChangeDao statusChangeDao = mock(OrderStatusChangeDao.class);
    private final SecurityIdentity securityIdentity = mock(SecurityIdentity.class);
    private final OrderService service = new OrderServiceImpl(pool, orderDao, positionDao, statusChangeDao,
            securityIdentity);

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
                        Order.builder().id(UUID.randomUUID()).build(),
                        Order.builder().id(UUID.randomUUID()).build(),
                        Order.builder().id(UUID.randomUUID()).build())));
        final var subscriber = service.findAllOrders(pageSize, page).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        final var orders = subscriber.assertCompleted().getItem();
        assertNotNull(orders);
        assertEquals(3, orders.size());

        verify(orderDao, times(1))
                .findAllByUserName(eq(pool), eq(USERNAME), eq(pageSize), eq(page));
        verify(positionDao, times(3))
                .findAllByOrderId(eq(pool), any(UUID.class));
        verify(statusChangeDao, times(3))
                .findAllByOrderId(eq(pool), any(UUID.class));
    }

    @Test
    void createOrder_success() {
        // when
        when(orderDao.insert(eq(pool), any(UUID.class), anyString(), any(LocalDateTime.class)))
                .thenReturn(Uni.createFrom().voidItem());
        final var subscriber = service.createOrder().subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        subscriber.assertCompleted();

        verify(orderDao, times(1))
                .insert(eq(pool), any(UUID.class), eq(USERNAME), any(LocalDateTime.class));
    }

    @Test
    void addOrderPosition_success() {
        // given
        final var order = Order.builder()
                .id(ORDER_ID)
                .build();

        // when
        when(orderDao.findById(pool, ORDER_ID))
                .thenReturn(Uni.createFrom().item(order));
        when(positionDao.insert(eq(pool), eq(ORDER_ID), any(OrderPosition.class)))
                .thenReturn(Uni.createFrom().item(POSITION_ID));
        final var subscriber = service.addOrderPosition(ORDER_ID, PRODUCT_CODE, QUANTITY).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        subscriber.assertItem(POSITION_ID);
        verify(positionDao, times(1))
                .insert(eq(pool), eq(ORDER_ID), argThat(position -> PRODUCT_CODE.equals(position.getProductCode())
                        && QUANTITY == position.getQuantity()));
    }

    @Test
    void addOrderPosition_orderNotFound() {
        // when
        when(orderDao.findById(pool, ORDER_ID))
                .thenReturn(Uni.createFrom().failure(new NoSuchElementException()));
        final var subscriber = service.addOrderPosition(ORDER_ID, PRODUCT_CODE, QUANTITY).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        final var failure = (OrderNotFoundException) subscriber
                .assertFailedWith(OrderNotFoundException.class)
                .getFailure();
        assertEquals(ORDER_ID, failure.getOrderId());

        verify(positionDao, never())
                .insert(eq(pool), eq(ORDER_ID), any(OrderPosition.class));
    }

    @Test
    void removeOrderPosition_success() {
        // given
        final var position = OrderPosition.builder()
                .productCode(PRODUCT_CODE)
                .quantity(QUANTITY)
                .build();

        // when
        when(positionDao.findById(pool, POSITION_ID))
                .thenReturn(Uni.createFrom().item(position));
        final var subscriber = service.removeOrderPosition(POSITION_ID).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        subscriber.assertCompleted();
        verify(positionDao, times(1))
                .deleteById(pool, POSITION_ID);
    }

    @Test
    void removeOrderPosition_positionNotFound() {
        // when
        when(positionDao.findById(pool, POSITION_ID))
                .thenReturn(Uni.createFrom().failure(new NoSuchElementException()));
        final var subscriber = service.removeOrderPosition(POSITION_ID).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        subscriber.assertFailedWith(OrderPositionNotFoundException.class);
        verify(positionDao, never()).deleteById(pool, POSITION_ID);
    }

    @Test
    void placeOrder_success() {
        // given
        final var order = Order.builder()
                .id(ORDER_ID)
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .userName(USERNAME)
                .build();

        // when
        when(orderDao.findById(pool, ORDER_ID))
                .thenReturn(Uni.createFrom().item(order));
        final var subscriber = service.placeOrder(ORDER_ID).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        subscriber.assertCompleted();

        verify(statusChangeDao, times(1))
                .insert(eq(pool), eq(ORDER_ID),
                        argThat(statusChange -> OrderStatus.PLACED.equals(statusChange.getStatus())));
        verify(orderDao, times(1))
                .updateStatus(pool, ORDER_ID, OrderStatus.PLACED);
    }

    @Test
    void placeOrder_notFound() {
        // when
        when(orderDao.findById(eq(pool), any(UUID.class)))
                .thenReturn(Uni.createFrom().failure(new NoSuchElementException("order not found")));
        final var subscriber = service.placeOrder(ORDER_ID).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        final OrderNotFoundException failure = (OrderNotFoundException) subscriber
                .assertFailedWith(OrderNotFoundException.class)
                .getFailure();
        assertEquals(ORDER_ID, failure.getOrderId());
    }

    @Test
    void placeOrder_createdByDifferentUser() {
        // given
        final var order = Order.builder()
                .id(ORDER_ID)
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .userName("other-user")
                .build();

        // when
        when(orderDao.findById(eq(pool), eq(ORDER_ID)))
                .thenReturn(Uni.createFrom().item(order));
        final var subscriber = service.placeOrder(ORDER_ID).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        final OrderCreatedByDifferentUserException failure = (OrderCreatedByDifferentUserException) subscriber
                .assertFailedWith(OrderCreatedByDifferentUserException.class)
                .getFailure();
        assertEquals(ORDER_ID, failure.getOrderId());
    }

    @Test
    void placeOrder_alreadyPlaced() {
        // given
        final var order = Order.builder()
                .id(ORDER_ID)
                .status(OrderStatus.PLACED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .userName(USERNAME)
                .build();

        // when
        when(orderDao.findById(eq(pool), eq(ORDER_ID)))
                .thenReturn(Uni.createFrom().item(order));
        final var subscriber = service.placeOrder(ORDER_ID).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        final OrderAlreadyPlacedException failure = (OrderAlreadyPlacedException) subscriber
                .assertFailedWith(OrderAlreadyPlacedException.class)
                .getFailure();
        assertEquals(ORDER_ID, failure.getOrderId());
    }

    @Test
    void cancelOrder_success() {
        // given
        final var order = Order.builder()
                .id(ORDER_ID)
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .userName(USERNAME)
                .build();

        // when
        when(orderDao.findById(pool, ORDER_ID))
                .thenReturn(Uni.createFrom().item(order));
        final var subscriber = service.cancelOrder(ORDER_ID).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        subscriber.assertCompleted();

        verify(statusChangeDao, times(1))
                .insert(eq(pool), eq(ORDER_ID),
                        argThat(statusChange -> OrderStatus.CANCELED.equals(statusChange.getStatus())));
        verify(orderDao, times(1))
                .updateStatus(pool, ORDER_ID, OrderStatus.CANCELED);
    }

    @Test
    void cancelOrder_notFound() {
        // when
        when(orderDao.findById(eq(pool), any(UUID.class)))
                .thenReturn(Uni.createFrom().failure(new NoSuchElementException("order not found")));
        final var subscriber = service.cancelOrder(ORDER_ID).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        final OrderNotFoundException failure = (OrderNotFoundException) subscriber
                .assertFailedWith(OrderNotFoundException.class)
                .getFailure();
        assertEquals(ORDER_ID, failure.getOrderId());
    }

    @Test
    void cancelOrder_createdByDifferentUser() {
        // given
        final var order = Order.builder()
                .id(ORDER_ID)
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .userName("other-user")
                .build();

        // when
        when(orderDao.findById(eq(pool), eq(ORDER_ID)))
                .thenReturn(Uni.createFrom().item(order));
        final var subscriber = service.cancelOrder(ORDER_ID).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        final OrderCreatedByDifferentUserException failure = (OrderCreatedByDifferentUserException) subscriber
                .assertFailedWith(OrderCreatedByDifferentUserException.class)
                .getFailure();
        assertEquals(ORDER_ID, failure.getOrderId());
    }

    @Test
    void cancelOrder_canNotBeCanceled() {
        // given
        final var order = Order.builder()
                .id(ORDER_ID)
                .status(OrderStatus.PLACED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .userName(USERNAME)
                .build();

        // when
        when(orderDao.findById(eq(pool), eq(ORDER_ID)))
                .thenReturn(Uni.createFrom().item(order));
        final var subscriber = service.cancelOrder(ORDER_ID).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        final OrderCanNotBeCanceledException failure = (OrderCanNotBeCanceledException) subscriber
                .assertFailedWith(OrderCanNotBeCanceledException.class)
                .getFailure();
        assertEquals(ORDER_ID, failure.getOrderId());
    }
}
