package org.auwerk.otus.arch.orderservice.service;

import io.smallrye.mutiny.Uni;

public interface ProductService {
    
    Uni<Boolean> checkProductAvailability(String productCode);
}
