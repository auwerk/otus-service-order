package org.auwerk.otus.arch.orderservice.domain;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderStatusChange {
    private OrderStatus status;
    private LocalDateTime createdAt;
}
