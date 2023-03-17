package org.auwerk.otus.arch.orderservice.mapper;

import org.auwerk.otus.arch.orderservice.api.dto.OrderDto;
import org.auwerk.otus.arch.orderservice.domain.Order;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface OrderMapper {

    OrderDto toDto(Order order);
}
