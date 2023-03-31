package org.auwerk.otus.arch.orderservice.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import org.auwerk.arch.reactivesaga.Saga;
import org.auwerk.arch.reactivesaga.log.InMemoryExecutionLog;
import org.auwerk.otus.arch.orderservice.dao.OrderDao;
import org.auwerk.otus.arch.orderservice.dao.OrderPositionDao;
import org.auwerk.otus.arch.orderservice.dao.OrderStatusChangeDao;
import org.auwerk.otus.arch.orderservice.domain.Order;
import org.auwerk.otus.arch.orderservice.domain.OrderPosition;
import org.auwerk.otus.arch.orderservice.domain.OrderStatus;
import org.auwerk.otus.arch.orderservice.domain.OrderStatusChange;
import org.auwerk.otus.arch.orderservice.exception.OrderAlreadyPlacedException;
import org.auwerk.otus.arch.orderservice.exception.OrderCanNotBeCanceledException;
import org.auwerk.otus.arch.orderservice.exception.OrderCanNotBeChangedException;
import org.auwerk.otus.arch.orderservice.exception.OrderCreatedByDifferentUserException;
import org.auwerk.otus.arch.orderservice.exception.OrderIsNotPlacedException;
import org.auwerk.otus.arch.orderservice.exception.OrderNotFoundException;
import org.auwerk.otus.arch.orderservice.exception.OrderPositionNotFoundException;
import org.auwerk.otus.arch.orderservice.service.BillingService;
import org.auwerk.otus.arch.orderservice.service.LicenseService;
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
    private final LicenseService licenseService;
    private final BillingService billingService;

    @Override
    public Uni<List<Order>> getAllOrders(int pageSize, int page) {
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
    public Uni<Order> getOrderById(UUID id) {
        return orderDao.findById(pool, id)
                .invoke(order -> {
                    if (!securityIdentity.getPrincipal().getName().equals(order.getUserName())) {
                        throw new OrderCreatedByDifferentUserException(order.getId());
                    }
                })
                .call(order -> Uni.combine().all().unis(
                        positionDao.findAllByOrderId(pool, order.getId())
                                .invoke(positions -> order.setPositions(positions)),
                        statusChangeDao.findAllByOrderId(pool, order.getId())
                                .invoke(statusChanges -> order.setStatusChanges(statusChanges)))
                        .discardItems())
                .onFailure(NoSuchElementException.class)
                .transform(ex -> new OrderNotFoundException(id));
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
                .orderId(orderId)
                .productCode(productCode)
                .quantity(quantity)
                .build();

        return pool.withTransaction(conn -> orderDao.findById(pool, orderId)
                .invoke(order -> {
                    if (!OrderStatus.CREATED.equals(order.getStatus())) {
                        throw new OrderCanNotBeChangedException(orderId);
                    }
                })
                .call(() -> productService.getProductPrice(productCode)
                        .invoke(price -> position.setPrice(price)))
                .flatMap(order -> positionDao.insert(pool, position))
                .onFailure(NoSuchElementException.class)
                .transform(ex -> new OrderNotFoundException(orderId)));
    }

    @Override
    public Uni<Void> removeOrderPosition(UUID positionId) {
        return positionDao.findById(pool, positionId)
                .call(position -> orderDao.findById(pool, position.getOrderId())
                        .invoke(order -> {
                            if (!OrderStatus.CREATED.equals(order.getStatus())) {
                                throw new OrderCanNotBeChangedException(order.getId());
                            }
                        }))
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
                .call(order -> positionDao.findAllByOrderId(pool, order.getId())
                        .invoke(positions -> order.setPositions(positions)))
                .call(order -> {
                    final var positionUnis = order.getPositions().stream()
                            .map(position -> productService.getProductPrice(position.getProductCode())
                                    .call(price -> positionDao.updatePriceById(pool, position.getId(), price)))
                            .toList();
                    if (positionUnis.isEmpty()) {
                        return Uni.createFrom().voidItem();
                    }
                    return Uni.combine().all().unis(positionUnis).discardItems();
                })
                .call(order -> Uni.combine().all().unis(
                        insertOrderStatusChange(pool, order.getId(), OrderStatus.PLACED),
                        orderDao.updateStatus(pool, order.getId(), OrderStatus.PLACED)).discardItems())
                .replaceWithVoid()
                .onFailure(NoSuchElementException.class)
                .transform(ex -> new OrderNotFoundException(orderId)));
    }

    @Override
    public Uni<Void> payOrder(UUID orderId) {
        return pool.withTransaction(conn -> orderDao.findById(pool, orderId)
                .invoke(order -> {
                    if (!order.getUserName().equals(securityIdentity.getPrincipal().getName())) {
                        throw new OrderCreatedByDifferentUserException(order.getId());
                    }
                    if (!OrderStatus.PLACED.equals(order.getStatus())) {
                        throw new OrderIsNotPlacedException(order.getId());
                    }
                })
                .call(order -> positionDao.findAllByOrderId(pool, order.getId())
                        .invoke(positions -> order.setPositions(positions)))
                .call(order -> {
                    if (order.getPositions().isEmpty()) {
                        return Uni.createFrom().voidItem();
                    }

                    final var sagaExectionLog = new InMemoryExecutionLog();
                    final var saga = new Saga(sagaExectionLog);

                    saga.addStory(
                            context -> {
                                return billingService
                                        .withdrawFunds(calculateTotal(order.getPositions()),
                                                "payment for order ID=" + order.getId())
                                        .invoke(operationId -> context.getValues().put("operationId", operationId))
                                        .replaceWithVoid();
                            },
                            context -> billingService.cancelOperation(context.getValue("operationId")));
                    order.getPositions().forEach(position -> saga.addStory(
                            context -> licenseService.createLicense(position.getProductCode())
                                    .invoke(licenseId -> context.getValues().put("licenseId", licenseId))
                                    .replaceWithVoid(),
                            context -> licenseService.deleteLicense(context.getValue("licenseId"))));

                    return saga.execute();
                })
                .call(order -> Uni.combine().all().unis(
                        insertOrderStatusChange(pool, order.getId(), OrderStatus.COMPLETED),
                        orderDao.updateStatus(pool, order.getId(), OrderStatus.COMPLETED)).discardItems())
                .onFailure(NoSuchElementException.class)
                .transform(ex -> new OrderNotFoundException(orderId))
                .replaceWithVoid());
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

    protected static BigDecimal calculateTotal(List<OrderPosition> positions) {
        var total = BigDecimal.ZERO;
        for (final var position : positions) {
            total = total.add(position.getPrice()
                    .multiply(BigDecimal.valueOf(position.getQuantity())));
        }
        return total;
    }

    private Uni<Void> insertOrderStatusChange(PgPool pool, UUID orderId, OrderStatus targetStatus) {
        final var statusChange = OrderStatusChange.builder()
                .status(targetStatus)
                .createdAt(LocalDateTime.now())
                .build();

        return statusChangeDao.insert(pool, orderId, statusChange);
    }
}
