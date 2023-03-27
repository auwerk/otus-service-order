package org.auwerk.otus.arch.orderservice.api.dto;

import java.util.UUID;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

@Data
@RegisterForReflection
public class AddOrderPositionRequestDto {
    private UUID orderId;
    private String productCode;
    private Integer quantity;
}
