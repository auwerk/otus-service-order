package org.auwerk.otus.arch.orderservice.service.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.auwerk.otus.arch.orderservice.dao.OrderDao;
import org.auwerk.otus.arch.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

public class OrderServiceImplTest {

    private final OrderDao dao = mock(OrderDao.class);
    private final OrderService service = new OrderServiceImpl(dao);

    @Test
    void createOrder_success() {
        final var orderId = UUID.randomUUID();
        when(dao.insertOrder()).thenReturn(Uni.createFrom().item(orderId));

        service.createOrder().subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(orderId);
    }
}
