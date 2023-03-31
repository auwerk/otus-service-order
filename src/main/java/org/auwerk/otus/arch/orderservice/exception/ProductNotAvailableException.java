package org.auwerk.otus.arch.orderservice.exception;

import lombok.Getter;

public class ProductNotAvailableException extends RuntimeException {

    @Getter
    private final String productCode;

    public ProductNotAvailableException(String productCode) {
        super("product not available, code=" + productCode);
        this.productCode = productCode;
    }
}
