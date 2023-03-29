package org.auwerk.otus.arch.orderservice.api;

import java.util.UUID;

import org.auwerk.otus.arch.orderservice.api.dto.AddOrderPositionRequestDto;
import org.auwerk.otus.arch.orderservice.exception.OrderCanNotBeChangedException;
import org.auwerk.otus.arch.orderservice.exception.OrderNotFoundException;
import org.auwerk.otus.arch.orderservice.exception.OrderPositionNotFoundException;
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
@TestHTTPEndpoint(OrderPositionResource.class)
public class OrderPositionResourceTest extends AbstractAuthenticatedResourceTest {

    private static final String USERNAME = "customer";
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID POSITION_ID = UUID.randomUUID();
    private static final String PRODUCT_CODE = "PRODUCT1";
    private static final int QUANTITY = 16;

    @InjectMock
    OrderService orderService;

    @Test
    void addOrderPosition_success() {
        Mockito.when(orderService.addOrderPosition(ORDER_ID, PRODUCT_CODE, QUANTITY))
                .thenReturn(Uni.createFrom().item(POSITION_ID));

        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .contentType(ContentType.JSON)
                .body(buildAddRequest())
                .post()
                .then()
                .statusCode(200)
                .body("positionId", Matchers.equalTo(POSITION_ID.toString()));
    }

    @Test
    void addOrderPosition_orderNotFound() {
        Mockito.when(orderService.addOrderPosition(ORDER_ID, PRODUCT_CODE, QUANTITY))
                .thenReturn(Uni.createFrom().failure(new OrderNotFoundException(ORDER_ID)));

        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .contentType(ContentType.JSON)
                .body(buildAddRequest())
                .post()
                .then()
                .statusCode(404)
                .body(Matchers.equalTo("order not found, id=" + ORDER_ID));
    }

    @Test
    void addOrderPosition_productNotAvailable() {
        Mockito.when(orderService.addOrderPosition(ORDER_ID, PRODUCT_CODE, QUANTITY))
                .thenReturn(Uni.createFrom().failure(new ProductNotAvailableException(PRODUCT_CODE)));

        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .contentType(ContentType.JSON)
                .body(buildAddRequest())
                .post()
                .then()
                .statusCode(409)
                .body(Matchers.equalTo("product not available, code=" + PRODUCT_CODE));
    }

    @Test
    void addOrderPosition_orderCanNotBeChanged() {
        Mockito.when(orderService.addOrderPosition(ORDER_ID, PRODUCT_CODE, QUANTITY))
                .thenReturn(Uni.createFrom().failure(new OrderCanNotBeChangedException(ORDER_ID)));

        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .contentType(ContentType.JSON)
                .body(buildAddRequest())
                .post()
                .then()
                .statusCode(403)
                .body(Matchers.equalTo("order can not be changed, id=" + ORDER_ID));
    }

    @Test
    void removeOrderPosition_success() {
        Mockito.when(orderService.removeOrderPosition(POSITION_ID))
                .thenReturn(Uni.createFrom().voidItem());

        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .delete("/{positionId}", POSITION_ID)
                .then()
                .statusCode(200);
    }

    @Test
    void removeOrderPosition_positionNotFound() {
        Mockito.when(orderService.removeOrderPosition(POSITION_ID))
                .thenReturn(Uni.createFrom().failure(new OrderPositionNotFoundException(POSITION_ID)));

        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .delete("/{positionId}", POSITION_ID)
                .then()
                .statusCode(404)
                .body(Matchers.equalTo("order position not found, id=" + POSITION_ID));
    }

    @Test
    void removeOrderPosition_orderCanNotBeChanged() {
        Mockito.when(orderService.removeOrderPosition(POSITION_ID))
                .thenReturn(Uni.createFrom().failure(new OrderCanNotBeChangedException(ORDER_ID)));

        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .delete("/{positionId}", POSITION_ID)
                .then()
                .statusCode(403)
                .body(Matchers.equalTo("order can not be changed, id=" + ORDER_ID));
    }

    private static AddOrderPositionRequestDto buildAddRequest() {
        final var request = new AddOrderPositionRequestDto();
        request.setOrderId(ORDER_ID);
        request.setProductCode(PRODUCT_CODE);
        request.setQuantity(QUANTITY);

        return request;
    }
}
