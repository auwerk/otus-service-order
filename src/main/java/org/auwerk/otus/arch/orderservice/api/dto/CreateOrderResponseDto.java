package org.auwerk.otus.arch.orderservice.api.dto;

import java.util.UUID;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@RegisterForReflection
public class CreateOrderResponseDto {
    private final UUID orderId;
}
