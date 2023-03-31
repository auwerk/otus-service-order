package org.auwerk.otus.arch.orderservice.service.impl;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.auwerk.otus.arch.orderservice.client.LicenseServiceClient;
import org.auwerk.otus.arch.orderservice.client.dto.license.CreateLicenseRequestDto;
import org.auwerk.otus.arch.orderservice.service.LicenseService;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class LicenseServiceImpl implements LicenseService {

    @Inject
    @RestClient
    LicenseServiceClient licenseServiceClient;

    @Override
    public Uni<UUID> createLicense(String productCode) {
        return licenseServiceClient
                .createLicense(new CreateLicenseRequestDto(UUID.randomUUID(), productCode))
                .map(response -> response.getLicenseId());
    }
}
