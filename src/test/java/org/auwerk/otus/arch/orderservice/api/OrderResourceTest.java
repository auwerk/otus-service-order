package org.auwerk.otus.arch.orderservice.api;

import java.util.List;
import java.util.UUID;

import org.auwerk.otus.arch.orderservice.client.exception.billing.InsufficentFundsException;
import org.auwerk.otus.arch.orderservice.domain.Order;
import org.auwerk.otus.arch.orderservice.exception.OrderAlreadyPlacedException;
import org.auwerk.otus.arch.orderservice.exception.OrderCreatedByDifferentUserException;
import org.auwerk.otus.arch.orderservice.exception.OrderIsNotPlacedException;
import org.auwerk.otus.arch.orderservice.exception.OrderNotFoundException;
import org.auwerk.otus.arch.orderservice.exception.ProductNotAvailableException;
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

@QuarkusTest
@TestHTTPEndpoint(OrderResource.class)
public class OrderResourceTest extends AbstractAuthenticatedResourceTest {

    private static final String USERNAME = "customer";
    private static final UUID ORDER_ID = UUID.randomUUID();

    @InjectMock
    OrderService orderService;

    @Test
    void getAllOrders() {
        Mockito.when(orderService.getAllOrders(10, 1))
                .thenReturn(Uni.createFrom().item(List.of(
                        Order.builder().build(),
                        Order.builder().build())));

        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .param("pageSize", 10)
                .param("page", 1)
                .get()
                .then()
                .statusCode(200);
    }

    @Test
    void getAllOrders_defaultPageParams() {
        Mockito.when(orderService.getAllOrders(10, 1))
                .thenReturn(Uni.createFrom().item(List.of(
                        Order.builder().build(),
                        Order.builder().build())));

        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .get()
                .then()
                .statusCode(200);

        Mockito.verify(orderService, Mockito.times(1))
                .getAllOrders(Integer.valueOf(OrderResource.DEFAULT_PAGE_SIZE),
                        Integer.valueOf(OrderResource.DEFAULT_PAGE));
    }

    @Test
    void getOrderById_success() {
        // given
        final var order = Order.builder().build();

        // when
        Mockito.when(orderService.getOrderById(ORDER_ID))
                .thenReturn(Uni.createFrom().item(order));

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .get("/{orderId}", ORDER_ID)
                .then()
                .statusCode(200);
    }

    @Test
    void getOrderById_orderNotFound() {
        // when
        Mockito.when(orderService.getOrderById(ORDER_ID))
                .thenReturn(Uni.createFrom().failure(new OrderNotFoundException(ORDER_ID)));

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .get("/{orderId}", ORDER_ID)
                .then()
                .statusCode(404)
                .body(Matchers.equalTo("order not found, id=" + ORDER_ID));
    }

    @Test
    void getOrderById_createdByDifferentUser() {
        // when
        Mockito.when(orderService.getOrderById(ORDER_ID))
                .thenReturn(Uni.createFrom()
                        .failure(new OrderCreatedByDifferentUserException(ORDER_ID)));

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .get("/{orderId}", ORDER_ID)
                .then()
                .statusCode(403)
                .body(Matchers.equalTo("order was created by different user, id=" + ORDER_ID));
    }

    @Test
    void createOrder_success() {
        // when
        Mockito.when(orderService.createOrder())
                .thenReturn(Uni.createFrom().item(ORDER_ID));

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .contentType(ContentType.JSON)
                .post()
                .then()
                .statusCode(200)
                .body("orderId", Matchers.equalTo(ORDER_ID.toString()));
    }

    @Test
    void placeOrder_success() {
        // when
        Mockito.when(orderService.placeOrder(ORDER_ID))
                .thenReturn(Uni.createFrom().voidItem());

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .put("/{orderId}/place", ORDER_ID)
                .then()
                .statusCode(200);
    }

    @Test
    void placeOrder_notFound() {
        // when
        Mockito.when(orderService.placeOrder(ORDER_ID))
                .thenReturn(Uni.createFrom().failure(new OrderNotFoundException(ORDER_ID)));

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .put("/{orderId}/place", ORDER_ID)
                .then()
                .statusCode(404)
                .body(Matchers.equalTo("order not found, id=" + ORDER_ID));
    }

    @Test
    void placeOrder_createdByDifferentUser() {
        // when
        Mockito.when(orderService.placeOrder(ORDER_ID))
                .thenReturn(Uni.createFrom()
                        .failure(new OrderCreatedByDifferentUserException(ORDER_ID)));

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .put("/{orderId}/place", ORDER_ID)
                .then()
                .statusCode(403)
                .body(Matchers.equalTo("order was created by different user, id=" + ORDER_ID));
    }

    @Test
    void placeOrder_alreadyPlaced() {
        // when
        Mockito.when(orderService.placeOrder(ORDER_ID))
                .thenReturn(Uni.createFrom().failure(new OrderAlreadyPlacedException(ORDER_ID)));

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .put("/{orderId}/place", ORDER_ID)
                .then()
                .statusCode(409)
                .body(Matchers.equalTo("order has been already placed, id=" + ORDER_ID));
    }

    @Test
    void placeOrder_productNotAvailable() {
        // given
        final var productCode = "PRODUCT1";

        // when
        Mockito.when(orderService.placeOrder(ORDER_ID))
                .thenReturn(Uni.createFrom().failure(new ProductNotAvailableException(productCode)));

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .put("/{orderId}/place", ORDER_ID)
                .then()
                .statusCode(409)
                .body(Matchers.equalTo("product not available, code=" + productCode));
    }

    @Test
    void payOrder_success() {
        // when
        Mockito.when(orderService.payOrder(ORDER_ID))
                .thenReturn(Uni.createFrom().voidItem());

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .put("/{orderId}/pay", ORDER_ID)
                .then()
                .statusCode(200);
    }

    @Test
    void payOrder_notFound() {
        // when
        Mockito.when(orderService.payOrder(ORDER_ID))
                .thenReturn(Uni.createFrom().failure(new OrderNotFoundException(ORDER_ID)));

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .put("/{orderId}/pay", ORDER_ID)
                .then()
                .statusCode(404)
                .body(Matchers.equalTo("order not found, id=" + ORDER_ID));
    }

    @Test
    void payOrder_createdByDifferentUser() {
        // when
        Mockito.when(orderService.payOrder(ORDER_ID))
                .thenReturn(Uni.createFrom()
                        .failure(new OrderCreatedByDifferentUserException(ORDER_ID)));

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .put("/{orderId}/pay", ORDER_ID)
                .then()
                .statusCode(403)
                .body(Matchers.equalTo("order was created by different user, id=" + ORDER_ID));
    }

    @Test
    void payOrder_notPlaced() {
        // when
        Mockito.when(orderService.payOrder(ORDER_ID))
                .thenReturn(Uni.createFrom().failure(new OrderIsNotPlacedException(ORDER_ID)));

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .put("/{orderId}/pay", ORDER_ID)
                .then()
                .statusCode(409)
                .body(Matchers.equalTo("order is not placed, id=" + ORDER_ID));
    }

    @Test
    void payOrder_insufficentFunds() {
        // given
        final var propagatedErrorMessage = "insufficent funds";

        // when
        Mockito.when(orderService.payOrder(ORDER_ID))
                .thenReturn(Uni.createFrom().failure(new InsufficentFundsException(propagatedErrorMessage)));

        // then
        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .put("/{orderId}/pay", ORDER_ID)
                .then()
                .statusCode(403)
                .body(Matchers.is(propagatedErrorMessage));
    }
}
