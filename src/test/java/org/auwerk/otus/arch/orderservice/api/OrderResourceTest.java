package org.auwerk.otus.arch.orderservice.api;

import java.util.List;
import java.util.UUID;

import org.auwerk.otus.arch.orderservice.domain.Order;
import org.auwerk.otus.arch.orderservice.exception.OrderAlreadyPlacedException;
import org.auwerk.otus.arch.orderservice.exception.OrderCreatedByDifferentUserException;
import org.auwerk.otus.arch.orderservice.exception.OrderNotFoundException;
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

@QuarkusTest
@TestHTTPEndpoint(OrderResource.class)
public class OrderResourceTest {

    private static final UUID ORDER_ID = UUID.randomUUID();

    private final KeycloakTestClient keycloakTestClient = new KeycloakTestClient();

    @InjectMock
    OrderService orderService;

    @Test
    void listOrders() {
        Mockito.when(orderService.findAllOrders(10, 1))
                .thenReturn(Uni.createFrom().item(List.of(
                        Order.builder().build(),
                        Order.builder().build())));

        RestAssured.given()
                .auth().oauth2(getAccessToken())
                .param("pageSize", 10)
                .param("page", 1)
                .get()
                .then().statusCode(200);
    }

    @Test
    void createOrder_success() {
        // when
        Mockito.when(orderService.createOrder())
                .thenReturn(Uni.createFrom().item(ORDER_ID));

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken())
                .contentType(ContentType.JSON)
                .post()
                .then().statusCode(200)
                .assertThat()
                .body("orderId", Matchers.equalTo(ORDER_ID.toString()));
    }

    @Test
    void placeOrder_success() {
        // when
        Mockito.when(orderService.placeOrder(ORDER_ID))
                .thenReturn(Uni.createFrom().voidItem());

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken())
                .put("/" + ORDER_ID)
                .then().statusCode(200);
    }

    @Test
    void placeOrder_notFound() {
        // when
        Mockito.when(orderService.placeOrder(ORDER_ID))
                .thenReturn(Uni.createFrom().failure(new OrderNotFoundException(ORDER_ID)));

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken())
                .put("/" + ORDER_ID)
                .then().statusCode(404);
    }

    @Test
    void placeOrder_createdByDifferentUser() {
        // when
        Mockito.when(orderService.placeOrder(ORDER_ID))
                .thenReturn(Uni.createFrom().failure(new OrderCreatedByDifferentUserException(ORDER_ID)));

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken())
                .put("/" + ORDER_ID)
                .then().statusCode(403);
    }

    @Test
    void placeOrder_alreadyPlaced() {
        // when
        Mockito.when(orderService.placeOrder(ORDER_ID))
                .thenReturn(Uni.createFrom().failure(new OrderAlreadyPlacedException(ORDER_ID)));

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken())
                .put("/" + ORDER_ID)
                .then().statusCode(409);
    }

    private String getAccessToken() {
        return keycloakTestClient.getAccessToken("bob");
    }
}
