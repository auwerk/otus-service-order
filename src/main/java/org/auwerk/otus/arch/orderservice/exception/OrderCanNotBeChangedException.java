package org.auwerk.otus.arch.orderservice.exception;

import java.util.UUID;

import lombok.Getter;

public class OrderCanNotBeChangedException extends RuntimeException {

    @Getter
    private final UUID orderId;

    public OrderCanNotBeChangedException(UUID orderId) {
        super("order can not be changed, id=" + orderId);
        this.orderId = orderId;
    }
}
