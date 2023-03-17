package org.auwerk.otus.arch.orderservice.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.auwerk.otus.arch.orderservice.api.dto.CreateOrderResponseDto;
import org.auwerk.otus.arch.orderservice.api.dto.PlaceOrderRequestDto;
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
                .map(orderId -> Response.ok(CreateOrderResponseDto.builder()
                        .orderId(orderId)
                        .build()).build());
    }

    @PUT
    public Uni<Response> placeOrder(PlaceOrderRequestDto requestDto) {
        return orderService
                .placeOrder(requestDto.getOrderId(), requestDto.getProductCode(), requestDto.getQuantity())
                .map(placedOrder -> Response.ok(orderMapper.toDto(placedOrder)).build());
    }
}
