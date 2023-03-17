package org.auwerk.otus.arch.orderservice.exception;

import java.util.UUID;

import lombok.Getter;

public class OrderNotFoundException extends RuntimeException {

    @Getter
    private final UUID orderId;

    public OrderNotFoundException(UUID orderId) {
        super("order not found, id=" + orderId);
        this.orderId = orderId;
    }
}
