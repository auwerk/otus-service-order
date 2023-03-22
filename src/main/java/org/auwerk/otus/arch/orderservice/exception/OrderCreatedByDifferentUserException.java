package org.auwerk.otus.arch.orderservice.exception;

import java.util.UUID;

import lombok.Getter;

public class OrderCreatedByDifferentUserException extends RuntimeException {

    @Getter
    private final UUID orderId;
    
    public OrderCreatedByDifferentUserException(UUID orderId) {
        super("order was created by different user, id=" + orderId);
        this.orderId = orderId;
    }
}
