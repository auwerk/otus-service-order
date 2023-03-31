package org.auwerk.otus.arch.orderservice.client;

import java.util.UUID;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.auwerk.otus.arch.orderservice.client.dto.license.CreateLicenseRequestDto;
import org.auwerk.otus.arch.orderservice.client.dto.license.CreateLicenseResponseDto;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.oidc.token.propagation.AccessToken;
import io.smallrye.mutiny.Uni;

@RegisterRestClient(configKey = "license-service-api")
@AccessToken
@Path("/")
public interface LicenseServiceClient {

    @POST
    Uni<CreateLicenseResponseDto> createLicense(CreateLicenseRequestDto request);

    @DELETE
    @Path("/{licenseId}")
    Uni<Void> deleteLicense(@PathParam("licenseId") UUID licenseId);
}
