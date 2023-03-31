package org.auwerk.otus.arch.orderservice.client.dto.billing;

import java.math.BigDecimal;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class ExecuteOperationRequestDto {
    private OperationType type;
    private BigDecimal amount;
    private String comment;
}
