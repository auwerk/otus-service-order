package org.auwerk.otus.arch.orderservice.client.dto.license;

import java.util.UUID;

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
public class CreateLicenseRequestDto {
    private UUID queryId;
    private String productCode;
}
