package org.auwerk.otus.arch.orderservice.mapper;

import java.util.List;

import org.auwerk.otus.arch.orderservice.api.dto.OrderDto;
import org.auwerk.otus.arch.orderservice.domain.Order;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi", uses = OrderPositionMapper.class)
public interface OrderMapper {
    
    OrderDto toDto(Order order);

    List<OrderDto> toDtos(List<Order> orders);
}
