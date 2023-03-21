package org.auwerk.otus.arch.orderservice.api.dto;

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
    private String productCode;
    private Integer quantity;
}