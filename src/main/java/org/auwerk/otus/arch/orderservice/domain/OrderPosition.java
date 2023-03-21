package org.auwerk.otus.arch.orderservice.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderPosition {
    private String productCode;
    private Integer quantity;
}
