package org.auwerk.otus.arch.orderservice.api;

import java.util.UUID;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.auwerk.otus.arch.orderservice.api.dto.AddOrderPositionRequestDto;
import org.auwerk.otus.arch.orderservice.api.dto.AddOrderPositionResponseDto;
import org.auwerk.otus.arch.orderservice.exception.OrderNotFoundException;
import org.auwerk.otus.arch.orderservice.exception.OrderPositionNotFoundException;
import org.auwerk.otus.arch.orderservice.exception.ProductNotAvailableException;
import org.auwerk.otus.arch.orderservice.service.OrderService;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@Path("/position")
@RolesAllowed("${otus.role.customer}")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class OrderPositionResource {

    private final OrderService orderService;

    @POST
    public Uni<Response> addOrderPosition(AddOrderPositionRequestDto requestDto) {
        return orderService
                .addOrderPosition(requestDto.getOrderId(), requestDto.getProductCode(), requestDto.getQuantity())
                .map(positionId -> {
                    final var response = AddOrderPositionResponseDto.builder()
                            .positionId(positionId)
                            .build();
                    return Response.ok(response).build();
                })
                .onFailure(OrderNotFoundException.class)
                .recoverWithItem(failure -> Response.status(Status.NOT_FOUND).entity(failure.getMessage()).build())
                .onFailure(ProductNotAvailableException.class)
                .recoverWithItem(failure -> Response.status(Status.CONFLICT).entity(failure.getMessage()).build())
                .onFailure()
                .recoverWithItem(failure -> Response.serverError().entity(failure.getMessage()).build());
    }

    @DELETE
    @Path("/{positionId}")
    public Uni<Response> removeOrderPosition(@PathParam("positionId") UUID positionId) {
        return orderService.removeOrderPosition(positionId)
                .map(v -> Response.ok().build())
                .onFailure(OrderPositionNotFoundException.class)
                .recoverWithItem(failure -> Response.status(Status.NOT_FOUND).entity(failure.getMessage()).build())
                .onFailure()
                .recoverWithItem(failure -> Response.serverError().entity(failure.getMessage()).build());
    }
}
