package org.auwerk.otus.arch.orderservice.api;

import java.time.LocalDateTime;
import java.util.UUID;

import org.auwerk.otus.arch.orderservice.api.dto.PlaceOrderRequestDto;
import org.auwerk.otus.arch.orderservice.domain.Order;
import org.auwerk.otus.arch.orderservice.exception.OrderAlreadyPlacedException;
import org.auwerk.otus.arch.orderservice.exception.OrderNotFoundException;
import org.auwerk.otus.arch.orderservice.service.OrderService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;

@QuarkusTest
@TestHTTPEndpoint(OrderResource.class)
public class OrderResourceTest {

    private static final String PRODUCT_CODE = "PRODUCT1";
    private static final int QUANTITY = 16;

    @InjectMock
    OrderService orderService;

    @Test
    void createOrder_success() {
        // given
        final var orderId = UUID.randomUUID();
        final var createdAt = LocalDateTime.now();

        // when
        Mockito.when(orderService.createOrder())
                .thenReturn(Uni.createFrom().item(Tuple2.of(orderId, createdAt)));

        // then
        RestAssured.given().contentType(ContentType.JSON).when().post()
                .then().statusCode(200)
                .assertThat()
                .body("orderId", Matchers.equalTo(orderId.toString()))
                .and()
                .body("createdAt", Matchers.notNullValue());
    }

    @Test
    void placeOrder_success() {
        // given
        final var orderId = UUID.randomUUID();
        final var request = PlaceOrderRequestDto.builder()
                .productCode(PRODUCT_CODE)
                .quantity(QUANTITY)
                .build();

        // when
        Mockito.when(orderService.placeOrder(orderId, PRODUCT_CODE, QUANTITY))
                .thenReturn(Uni.createFrom().item(Order.builder()
                        .id(orderId)
                        .createdAt(LocalDateTime.now())
                        .productCode(PRODUCT_CODE)
                        .quantity(QUANTITY)
                        .placedAt(LocalDateTime.now())
                        .build()));

        // then
        RestAssured.given().contentType(ContentType.JSON).body(request).put("/" + orderId)
                .then().statusCode(200)
                .assertThat()
                .body("id", Matchers.equalTo(orderId.toString()))
                .and()
                .body("productCode", Matchers.equalTo(PRODUCT_CODE))
                .and()
                .body("quantity", Matchers.equalTo(QUANTITY))
                .and()
                .body("createdAt", Matchers.notNullValue())
                .and()
                .body("placedAt", Matchers.notNullValue());
    }

    @Test
    void placeOrder_notFound() {
        // given
        final var orderId = UUID.randomUUID();
        final var request = PlaceOrderRequestDto.builder()
                .productCode(PRODUCT_CODE)
                .quantity(QUANTITY)
                .build();

        // when
        Mockito.when(orderService.placeOrder(orderId, PRODUCT_CODE, QUANTITY))
                .thenReturn(Uni.createFrom().failure(new OrderNotFoundException(orderId)));

        // then
        RestAssured.given().contentType(ContentType.JSON).body(request).put("/" + orderId)
                .then().statusCode(404);
    }

    @Test
    void placeOrder_alreadyPlaced() {
        // given
        final var orderId = UUID.randomUUID();
        final var request = PlaceOrderRequestDto.builder()
                .productCode(PRODUCT_CODE)
                .quantity(QUANTITY)
                .build();

        // when
        Mockito.when(orderService.placeOrder(orderId, PRODUCT_CODE, QUANTITY))
                .thenReturn(Uni.createFrom().failure(new OrderAlreadyPlacedException(orderId)));

        // then
        RestAssured.given().contentType(ContentType.JSON).body(request).put("/" + orderId)
                .then().statusCode(409);
    }
}
