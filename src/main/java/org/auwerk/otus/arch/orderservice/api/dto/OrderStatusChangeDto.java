package org.auwerk.otus.arch.orderservice.api.dto;

import java.time.LocalDateTime;

import org.auwerk.otus.arch.orderservice.domain.OrderStatus;

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
public class OrderStatusChangeDto {
    private OrderStatus status;
    private LocalDateTime createdAt;
}
