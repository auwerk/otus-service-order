package org.auwerk.otus.arch.orderservice.mapper;

import java.util.List;

import org.auwerk.otus.arch.orderservice.api.dto.OrderPositionDto;
import org.auwerk.otus.arch.orderservice.domain.OrderPosition;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface OrderPositionMapper {

    @Mapping(target = "orderId", ignore = true)
    OrderPosition fromDto(OrderPositionDto dto);

    List<OrderPosition> fromDtos(List<OrderPositionDto> dtos);

    OrderPositionDto toDto(OrderPosition position);

    List<OrderPositionDto> toDtos(List<OrderPosition> positions);
}
