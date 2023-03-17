package org.auwerk.otus.arch.orderservice.exception;

import java.util.UUID;

import lombok.Getter;

public class OrderAlreadyPlacedException extends RuntimeException {

    @Getter
    private final UUID orderId;

    public OrderAlreadyPlacedException(UUID orderId) {
        super("order has been already placed, id=" + orderId);
        this.orderId = orderId;
    }
}
