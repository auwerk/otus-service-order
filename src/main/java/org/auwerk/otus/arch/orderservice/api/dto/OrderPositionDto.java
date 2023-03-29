package org.auwerk.otus.arch.orderservice.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@RegisterForReflection
public class OrderPositionDto {
    private UUID id;
    private String productCode;
    private Integer quantity;
    private BigDecimal price;
}
