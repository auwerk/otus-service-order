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
    private String userName;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderPosition> positions;
    private List<OrderStatusChange> statusChanges;
}
