package org.auwerk.otus.arch.orderservice.domain;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderPosition {
    private UUID id;
    private UUID orderId;
    private String productCode;
    private Integer quantity;
    private BigDecimal price;
}
