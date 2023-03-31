package org.auwerk.otus.arch.orderservice.client.exception.billing;

public class InsufficentFundsException extends RuntimeException {

    public InsufficentFundsException(String message) {
        super(message);
    }
}
