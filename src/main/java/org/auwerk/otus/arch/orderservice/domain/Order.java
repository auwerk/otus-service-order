package org.auwerk.otus.arch.orderservice.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Order {
    private UUID id;
    private OrderStatus status;
    private String userName;
    private LocalDateTime createdAt;
    private LocalDateTime placedAt;
    private List<OrderPosition> positions;
}
