package org.auwerk.otus.arch.orderservice.service.impl;

import java.math.BigDecimal;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.auwerk.otus.arch.orderservice.client.ProductServiceClient;
import org.auwerk.otus.arch.orderservice.exception.ProductNotAvailableException;
import org.auwerk.otus.arch.orderservice.service.ProductService;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class ProductServiceImpl implements ProductService {

    @Inject
    @RestClient
    ProductServiceClient client;

    @Override
    public Uni<BigDecimal> getProductPrice(String productCode) {
        return client.getProductByCode(productCode).map(product -> {
            if (!Boolean.TRUE.equals(product.getAvailable())) {
                throw new ProductNotAvailableException(productCode);
            }
            return product.getPrice();
        });
    }
}
