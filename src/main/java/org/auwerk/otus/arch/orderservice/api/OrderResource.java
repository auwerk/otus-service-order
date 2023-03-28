package org.auwerk.otus.arch.orderservice.api;

import java.util.UUID;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.auwerk.otus.arch.orderservice.api.dto.CreateOrderResponseDto;
import org.auwerk.otus.arch.orderservice.exception.OrderAlreadyPlacedException;
import org.auwerk.otus.arch.orderservice.exception.OrderCanNotBeCanceledException;
import org.auwerk.otus.arch.orderservice.exception.OrderCreatedByDifferentUserException;
import org.auwerk.otus.arch.orderservice.exception.OrderNotFoundException;
import org.auwerk.otus.arch.orderservice.mapper.OrderMapper;
import org.auwerk.otus.arch.orderservice.service.OrderService;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@Path("/")
@RolesAllowed("${otus.role.customer}")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class OrderResource {

    protected static final String DEFAULT_PAGE_SIZE = "10";
    protected static final String DEFAULT_PAGE = "1";

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    @GET
    public Uni<Response> listOrders(@QueryParam("pageSize") @DefaultValue(DEFAULT_PAGE_SIZE) int pageSize,
            @QueryParam("page") @DefaultValue(DEFAULT_PAGE) int page) {
        return orderService.findAllOrders(pageSize, page)
                .map(orders -> Response.ok(orderMapper.toDtos(orders)).build())
                .onFailure()
                .recoverWithItem(failure -> Response.serverError().entity(failure.getMessage()).build());
    }

    @POST
    public Uni<Response> createOrder() {
        return orderService.createOrder()
                .map(orderId -> {
                    final var response = CreateOrderResponseDto.builder()
                            .orderId(orderId)
                            .build();
                    return Response.ok(response).build();
                })
                .onFailure()
                .recoverWithItem(failure -> Response.serverError().entity(failure.getMessage()).build());
    }

    @PUT
    @Path("/{orderId}")
    public Uni<Response> placeOrder(@PathParam("orderId") UUID orderId) {
        return orderService
                .placeOrder(orderId)
                .map(placedOrder -> Response.ok().build())
                .onFailure(OrderNotFoundException.class)
                .recoverWithItem(Response.status(Status.NOT_FOUND).build())
                .onFailure(OrderCreatedByDifferentUserException.class)
                .recoverWithItem(Response.status(Status.FORBIDDEN).build())
                .onFailure(OrderAlreadyPlacedException.class)
                .recoverWithItem(Response.status(Status.CONFLICT).build())
                .onFailure()
                .recoverWithItem(failure -> Response.serverError().entity(failure.getMessage()).build());
    }

    @DELETE
    @Path("/{orderId}")
    public Uni<Response> cancelOrder(@PathParam("orderId") UUID orderId) {
        return orderService.cancelOrder(orderId)
                .replaceWith(Response.ok().build())
                .onFailure(OrderNotFoundException.class)
                .recoverWithItem(Response.status(Status.NOT_FOUND).build())
                .onFailure(OrderCreatedByDifferentUserException.class)
                .recoverWithItem(Response.status(Status.FORBIDDEN).build())
                .onFailure(OrderCanNotBeCanceledException.class)
                .recoverWithItem(Response.status(Status.CONFLICT).build())
                .onFailure()
                .recoverWithItem(failure -> Response.serverError().entity(failure.getMessage()).build());
    }
}
