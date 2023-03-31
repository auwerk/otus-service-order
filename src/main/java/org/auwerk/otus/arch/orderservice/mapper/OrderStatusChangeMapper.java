package org.auwerk.otus.arch.orderservice.mapper;

import java.util.List;

import org.auwerk.otus.arch.orderservice.api.dto.OrderStatusChangeDto;
import org.auwerk.otus.arch.orderservice.domain.OrderStatusChange;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface OrderStatusChangeMapper {

    OrderStatusChangeDto toDto(OrderStatusChange statusChange);

    List<OrderStatusChangeDto> toDtos(List<OrderStatusChange> statusChanges);
}
