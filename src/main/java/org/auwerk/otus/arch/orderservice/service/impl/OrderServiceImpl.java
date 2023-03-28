package org.auwerk.otus.arch.orderservice.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import org.auwerk.otus.arch.orderservice.dao.OrderDao;
import org.auwerk.otus.arch.orderservice.dao.OrderPositionDao;
import org.auwerk.otus.arch.orderservice.dao.OrderStatusChangeDao;
import org.auwerk.otus.arch.orderservice.domain.Order;
import org.auwerk.otus.arch.orderservice.domain.OrderPosition;
import org.auwerk.otus.arch.orderservice.domain.OrderStatus;
import org.auwerk.otus.arch.orderservice.domain.OrderStatusChange;
import org.auwerk.otus.arch.orderservice.exception.OrderAlreadyPlacedException;
import org.auwerk.otus.arch.orderservice.exception.OrderCanNotBeCanceledException;
import org.auwerk.otus.arch.orderservice.exception.OrderCreatedByDifferentUserException;
import org.auwerk.otus.arch.orderservice.exception.OrderNotFoundException;
import org.auwerk.otus.arch.orderservice.exception.OrderPositionNotFoundException;
import org.auwerk.otus.arch.orderservice.exception.ProductNotAvailableException;
import org.auwerk.otus.arch.orderservice.service.OrderService;
import org.auwerk.otus.arch.orderservice.service.ProductService;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final PgPool pool;
    private final OrderDao orderDao;
    private final OrderPositionDao positionDao;
    private final OrderStatusChangeDao statusChangeDao;
    private final SecurityIdentity securityIdentity;
    private final ProductService productService;

    @Override
    public Uni<List<Order>> findAllOrders(int pageSize, int page) {
        final var userName = securityIdentity.getPrincipal().getName();
        return orderDao.findAllByUserName(pool, userName, pageSize, page).call(orders -> {
            if (orders.isEmpty()) {
                return Uni.createFrom().item(List.of());
            } else {
                final var orderUnis = orders.stream()
                        .map(order -> Uni.combine().all().unis(
                                positionDao.findAllByOrderId(pool, order.getId())
                                        .invoke(positions -> order.setPositions(positions)),
                                statusChangeDao.findAllByOrderId(pool, order.getId())
                                        .invoke(statusChanges -> order.setStatusChanges(statusChanges)))
                                .discardItems())
                        .toList();
                return Uni.combine().all().unis(orderUnis).discardItems();
            }
        });
    }

    @Override
    public Uni<UUID> createOrder() {
        final var id = UUID.randomUUID();
        final var createdAt = LocalDateTime.now();
        final var userName = securityIdentity.getPrincipal().getName();

        return orderDao.insert(pool, id, userName, createdAt)
                .chain(() -> insertOrderStatusChange(pool, id, OrderStatus.CREATED))
                .replaceWith(id);
    }

    @Override
    public Uni<UUID> addOrderPosition(UUID orderId, String productCode, Integer quantity) {
        final var position = OrderPosition.builder()
                .productCode(productCode)
                .quantity(quantity)
                .build();

        return pool.withTransaction(conn -> orderDao.findById(pool, orderId)
                .call(() -> productService.checkProductAvailability(productCode)
                        .invoke(available -> {
                            if (!Boolean.TRUE.equals(available)) {
                                throw new ProductNotAvailableException(productCode);
                            }
                        }))
                .flatMap(order -> positionDao.insert(pool, orderId, position))
                .onFailure(NoSuchElementException.class)
                .transform(ex -> new OrderNotFoundException(orderId)));
    }

    @Override
    public Uni<Void> removeOrderPosition(UUID positionId) {
        return positionDao.findById(pool, positionId)
                .flatMap(position -> positionDao.deleteById(pool, positionId))
                .onFailure(NoSuchElementException.class)
                .transform(ex -> new OrderPositionNotFoundException(positionId));
    }

    @Override
    public Uni<Void> placeOrder(UUID orderId) {
        return pool.withTransaction(conn -> orderDao.findById(pool, orderId)
                .invoke(order -> {
                    if (!order.getUserName().equals(securityIdentity.getPrincipal().getName())) {
                        throw new OrderCreatedByDifferentUserException(order.getId());
                    }
                    if (OrderStatus.PLACED.equals(order.getStatus())) {
                        throw new OrderAlreadyPlacedException(order.getId());
                    }
                })
                .call(order -> Uni.combine().all().unis(
                        insertOrderStatusChange(pool, order.getId(), OrderStatus.PLACED),
                        orderDao.updateStatus(pool, order.getId(), OrderStatus.PLACED)).discardItems())
                .replaceWithVoid()
                .onFailure(NoSuchElementException.class)
                .transform(ex -> new OrderNotFoundException(orderId)));
    }

    @Override
    public Uni<Void> cancelOrder(UUID orderId) {
        return pool.withTransaction(conn -> orderDao.findById(pool, orderId)
                .invoke(order -> {
                    if (!order.getUserName().equals(securityIdentity.getPrincipal().getName())) {
                        throw new OrderCreatedByDifferentUserException(order.getId());
                    }
                    if (!OrderStatus.CREATED.equals(order.getStatus())) {
                        throw new OrderCanNotBeCanceledException(order.getId());
                    }
                })
                .call(order -> Uni.combine().all().unis(
                        insertOrderStatusChange(pool, order.getId(), OrderStatus.CANCELED),
                        orderDao.updateStatus(pool, order.getId(), OrderStatus.CANCELED)).discardItems())
                .replaceWithVoid()
                .onFailure(NoSuchElementException.class)
                .transform(ex -> new OrderNotFoundException(orderId)));
    }

    private Uni<Void> insertOrderStatusChange(PgPool pool, UUID orderId, OrderStatus targetStatus) {
        final var statusChange = OrderStatusChange.builder()
                .status(targetStatus)
                .createdAt(LocalDateTime.now())
                .build();

        return statusChangeDao.insert(pool, orderId, statusChange);
    }
}
