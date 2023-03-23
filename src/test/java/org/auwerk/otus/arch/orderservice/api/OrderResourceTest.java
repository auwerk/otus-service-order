package org.auwerk.otus.arch.orderservice.api;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.auwerk.otus.arch.orderservice.api.dto.OrderPositionDto;
import org.auwerk.otus.arch.orderservice.api.dto.PlaceOrderRequestDto;
import org.auwerk.otus.arch.orderservice.domain.Order;
import org.auwerk.otus.arch.orderservice.exception.OrderAlreadyPlacedException;
import org.auwerk.otus.arch.orderservice.exception.OrderCreatedByDifferentUserException;
import org.auwerk.otus.arch.orderservice.exception.OrderNotFoundException;
import org.auwerk.otus.arch.orderservice.mapper.OrderPositionMapper;
import org.auwerk.otus.arch.orderservice.service.OrderService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;

@QuarkusTest
@TestHTTPEndpoint(OrderResource.class)
public class OrderResourceTest {

    private static final String PRODUCT_CODE = "PRODUCT1";
    private static final int QUANTITY = 16;

    private final KeycloakTestClient keycloakTestClient = new KeycloakTestClient();

    @InjectMock
    OrderService orderService;

    @Inject
    OrderPositionMapper positionMapper;

    @Test
    void listOrders() {
        Mockito.when(orderService.findAllOrders(10, 1))
                .thenReturn(Uni.createFrom().item(List.of(
                        Order.builder().build(),
                        Order.builder().build())));

        RestAssured.given()
                .auth().oauth2(getAccessToken())
                .param("pageSize", 10)
                .get("/1")
                .then().statusCode(200);
    }

    @Test
    void createOrder_success() {
        // given
        final var orderId = UUID.randomUUID();
        final var createdAt = LocalDateTime.now();

        // when
        Mockito.when(orderService.createOrder())
                .thenReturn(Uni.createFrom().item(Tuple2.of(orderId, createdAt)));

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken())
                .contentType(ContentType.JSON)
                .post()
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
        final var request = buildPlaceOrderRequest();

        // when
        Mockito.when(orderService.placeOrder(eq(orderId), anyList()))
                .thenReturn(Uni.createFrom().item(Order.builder()
                        .id(orderId)
                        .createdAt(LocalDateTime.now())
                        .positions(positionMapper.fromDtos(request.getPositions()))
                        .placedAt(LocalDateTime.now())
                        .build()));

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken())
                .contentType(ContentType.JSON).body(request)
                .put("/" + orderId)
                .then().statusCode(200)
                .assertThat()
                .body("id", Matchers.equalTo(orderId.toString()))
                .and()
                .body("positions", Matchers.hasSize(1))
                .and()
                .body("createdAt", Matchers.notNullValue())
                .and()
                .body("placedAt", Matchers.notNullValue());
    }

    @Test
    void placeOrder_notFound() {
        // given
        final var orderId = UUID.randomUUID();
        final var request = buildPlaceOrderRequest();

        // when
        Mockito.when(orderService.placeOrder(eq(orderId), anyList()))
                .thenReturn(Uni.createFrom().failure(new OrderNotFoundException(orderId)));

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken())
                .contentType(ContentType.JSON).body(request)
                .put("/" + orderId)
                .then().statusCode(404);
    }

    @Test
    void placeOrder_createdByDifferentUser() {
        // given
        final var orderId = UUID.randomUUID();
        final var request = buildPlaceOrderRequest();

        // when
        Mockito.when(orderService.placeOrder(eq(orderId), anyList()))
                .thenReturn(Uni.createFrom().failure(new OrderCreatedByDifferentUserException(orderId)));

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken())
                .contentType(ContentType.JSON).body(request)
                .put("/" + orderId)
                .then().statusCode(403);
    }

    @Test
    void placeOrder_alreadyPlaced() {
        // given
        final var orderId = UUID.randomUUID();
        final var request = buildPlaceOrderRequest();

        // when
        Mockito.when(orderService.placeOrder(eq(orderId), anyList()))
                .thenReturn(Uni.createFrom().failure(new OrderAlreadyPlacedException(orderId)));

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken())
                .contentType(ContentType.JSON).body(request).put("/" + orderId)
                .then().statusCode(409);
    }

    private static PlaceOrderRequestDto buildPlaceOrderRequest() {
        final var orderPositions = List.of(OrderPositionDto.builder()
                .productCode(PRODUCT_CODE)
                .quantity(QUANTITY)
                .build());
        return PlaceOrderRequestDto.builder()
                .positions(orderPositions)
                .build();
    }

    private String getAccessToken() {
        return keycloakTestClient.getAccessToken("bob");
    }
}
