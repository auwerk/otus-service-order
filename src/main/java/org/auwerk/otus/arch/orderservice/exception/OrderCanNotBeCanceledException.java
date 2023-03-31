package org.auwerk.otus.arch.orderservice.exception;

import java.util.UUID;

import lombok.Getter;

public class OrderCanNotBeCanceledException extends RuntimeException {

    @Getter
    private final UUID orderId;

    public OrderCanNotBeCanceledException(UUID orderId) {
        super("order can not be canceled, id=" + orderId);
        this.orderId = orderId;
    }
}
