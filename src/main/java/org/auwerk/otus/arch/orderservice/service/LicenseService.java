package org.auwerk.otus.arch.orderservice.service;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

public interface LicenseService {

    Uni<UUID> createLicense(String productCode);

    Uni<Void> deleteLicense(String productCode);
}
