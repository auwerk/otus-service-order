package org.auwerk.otus.arch.orderservice.client;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.auwerk.otus.arch.orderservice.client.dto.ProductDto;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;

@RegisterRestClient(configKey = "product-service-api")
public interface ProductServiceClient {

    @GET
    @Path("/{productCode}")
    Uni<ProductDto> getProductByCode(String productCode);
}
