package org.auwerk.otus.arch.orderservice.exception;

import java.util.UUID;

import lombok.Getter;

public class OrderIsNotPlacedException extends RuntimeException {

    @Getter
    private final UUID orderId;

    public OrderIsNotPlacedException(UUID orderId) {
        super("order is not placed, id=" + orderId);
        this.orderId = orderId;
    }
}
