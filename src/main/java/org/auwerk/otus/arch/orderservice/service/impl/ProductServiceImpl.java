package org.auwerk.otus.arch.orderservice.service.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.auwerk.otus.arch.orderservice.client.ProductServiceClient;
import org.auwerk.otus.arch.orderservice.service.ProductService;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class ProductServiceImpl implements ProductService {

    @Inject
    @RestClient
    ProductServiceClient client;

    @Override
    public Uni<Boolean> checkProductAvailability(String productCode) {
        return client.getProductByCode(productCode).map(product -> product.getAvailable());
    }
}
