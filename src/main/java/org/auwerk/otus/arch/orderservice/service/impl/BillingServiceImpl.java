package org.auwerk.otus.arch.orderservice.service.impl;

import java.math.BigDecimal;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.auwerk.otus.arch.orderservice.client.BillingServiceOperationClient;
import org.auwerk.otus.arch.orderservice.client.dto.billing.ExecuteOperationRequestDto;
import org.auwerk.otus.arch.orderservice.client.dto.billing.OperationType;
import org.auwerk.otus.arch.orderservice.service.BillingService;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class BillingServiceImpl implements BillingService {

    @Inject
    @RestClient
    BillingServiceOperationClient billingServiceOperationClient;

    @Override
    public Uni<UUID> withdrawFunds(BigDecimal amount, String comment) {
        return billingServiceOperationClient
                .executeOperation(new ExecuteOperationRequestDto(OperationType.WITHDRAW, amount, comment))
                .map(response -> response.getOperationId());
    }

    @Override
    public Uni<Void> cancelOperation(UUID operationId) {
        return billingServiceOperationClient.cancelOperation(operationId);
    }
}
