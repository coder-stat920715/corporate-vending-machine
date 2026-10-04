package com.corporate.vending.application.service;

import com.corporate.vending.application.dto.OrderResponse;
import com.corporate.vending.domain.model.OrderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "machineId", source = "machine.id")
    @Mapping(target = "slotNo", source = "slot.slotNo")
    @Mapping(target = "productName", source = "product.name")
    OrderResponse toResponse(OrderEntity order);
}
