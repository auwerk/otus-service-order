package org.auwerk.otus.arch.orderservice.api;

import io.quarkus.test.keycloak.client.KeycloakTestClient;

public abstract class AbstractAuthenticatedResourceTest {

    private final KeycloakTestClient keycloakTestClient = new KeycloakTestClient();

    protected String getAccessToken(String userName) {
        return keycloakTestClient.getAccessToken(userName);
    }
}
