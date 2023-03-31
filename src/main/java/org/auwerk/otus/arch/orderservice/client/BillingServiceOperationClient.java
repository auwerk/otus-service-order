package org.auwerk.otus.arch.orderservice.client;

import java.util.UUID;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.auwerk.otus.arch.orderservice.client.dto.billing.ExecuteOperationRequestDto;
import org.auwerk.otus.arch.orderservice.client.dto.billing.OperationResponseDto;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.oidc.token.propagation.AccessToken;
import io.smallrye.mutiny.Uni;

@RegisterRestClient(configKey = "billing-service-api")
@AccessToken
@Path("/operation")
public interface BillingServiceOperationClient {

    @POST
    Uni<OperationResponseDto> executeOperation(ExecuteOperationRequestDto request);

    @DELETE
    @Path("/{operationId}")
    Uni<Void> cancelOperation(@PathParam("operationId") UUID operationId);
}
