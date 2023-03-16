package org.auwerk.otus.arch.orderservice.service.impl;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import org.auwerk.otus.arch.orderservice.dao.OrderDao;
import org.auwerk.otus.arch.orderservice.service.OrderService;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderDao orderDao;

    @Override
    public Uni<UUID> createOrder() {
        return orderDao.insertOrder();
    }
}
