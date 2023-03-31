package org.auwerk.otus.arch.orderservice.service;

import java.math.BigDecimal;

import io.smallrye.mutiny.Uni;

public interface ProductService {

    Uni<BigDecimal> getProductPrice(String productCode);
}
