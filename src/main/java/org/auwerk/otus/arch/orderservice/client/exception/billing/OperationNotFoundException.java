package org.auwerk.otus.arch.orderservice.client.exception.billing;

public class OperationNotFoundException extends RuntimeException {

    public OperationNotFoundException(String message) {
        super(message);
    }
}
