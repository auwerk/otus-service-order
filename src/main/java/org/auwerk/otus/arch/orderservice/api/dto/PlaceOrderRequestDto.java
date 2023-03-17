package org.auwerk.otus.arch.orderservice.api.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

@Data
@RegisterForReflection
public class PlaceOrderRequestDto {
    private String productCode;
    private Integer quantity;
}
