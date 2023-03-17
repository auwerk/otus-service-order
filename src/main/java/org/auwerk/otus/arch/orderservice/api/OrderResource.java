package org.auwerk.otus.arch.orderservice.api;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.auwerk.otus.arch.orderservice.api.dto.CreateOrderResponseDto;
import org.auwerk.otus.arch.orderservice.api.dto.PlaceOrderRequestDto;
import org.auwerk.otus.arch.orderservice.exception.OrderAlreadyPlacedException;
import org.auwerk.otus.arch.orderservice.exception.OrderNotFoundException;
import org.auwerk.otus.arch.orderservice.mapper.OrderMapper;
import org.auwerk.otus.arch.orderservice.service.OrderService;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class OrderResource {

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    @POST
    public Uni<Response> createOrder() {
        return orderService.createOrder()
                .map(result -> {
                    final var dto = CreateOrderResponseDto.builder()
                            .orderId(result.getItem1())
                            .createdAt(result.getItem2())
                            .build();
                    return Response.ok(dto).build();
                });
    }

    @PUT
    @Path("/{orderId}")
    public Uni<Response> placeOrder(@PathParam("orderId") UUID orderId, PlaceOrderRequestDto requestDto) {
        return orderService
                .placeOrder(orderId, requestDto.getProductCode(), requestDto.getQuantity())
                .map(placedOrder -> Response.ok(orderMapper.toDto(placedOrder)).build())
                .onFailure(OrderNotFoundException.class)
                .recoverWithItem(Response.status(Status.NOT_FOUND).build())
                .onFailure(OrderAlreadyPlacedException.class)
                .recoverWithItem(Response.status(Status.CONFLICT).build());
    }
}
