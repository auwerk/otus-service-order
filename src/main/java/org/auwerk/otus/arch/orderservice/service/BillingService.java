package org.auwerk.otus.arch.orderservice.service;

import java.math.BigDecimal;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

public interface BillingService {

    Uni<UUID> withdrawFunds(BigDecimal amount, String comment);

    Uni<Void> cancelOperation(UUID operationId);
}
