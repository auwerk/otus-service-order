package org.auwerk.otus.arch.orderservice.service;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

public interface OrderService {

    Uni<UUID> createOrder();
    
}
