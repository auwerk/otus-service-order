package org.auwerk.otus.arch.orderservice.exception;

import java.util.UUID;

import lombok.Getter;

public class OrderPositionNotFoundException extends RuntimeException {

    @Getter
    private final UUID positionId;

    public OrderPositionNotFoundException(UUID positionId) {
        super("order position not found, id=" + positionId);
        this.positionId = positionId;
    }
}
