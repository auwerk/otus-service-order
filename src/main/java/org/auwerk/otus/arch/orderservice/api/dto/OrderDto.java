package org.auwerk.otus.arch.orderservice.api.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

@Data
@RegisterForReflection
public class OrderDto {
    private UUID id;
    private String status;
    private List<OrderPositionDto> positions;
    private LocalDateTime createdAt;
    private LocalDateTime placedAt;
}
