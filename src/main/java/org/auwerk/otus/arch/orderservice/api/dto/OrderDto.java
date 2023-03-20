package org.auwerk.otus.arch.orderservice.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

@Data
@RegisterForReflection
public class OrderDto {
    private UUID id;
    private String status;
    private String productCode;
    private Integer quantity;
    private LocalDateTime createdAt;
    private LocalDateTime placedAt;
}
