package com.example.ecommerce.order.mapper;

import com.example.ecommerce.order.dto.response.SubOrderResponse;
import com.example.ecommerce.order.entity.SubOrderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SubOrderMapper {

    @Mapping(target = "masterOrderId", source = "masterOrder.id")
    @Mapping(target = "managedByUserId", source = "managedBy.id")
    @Mapping(target = "managedByUsername", source = "managedBy.username")
    @Mapping(target = "approvedById", source = "approvedBy.id")
    @Mapping(target = "approvedByUsername", source = "approvedBy.username")
    @Mapping(target = "ticketId", source = "ticket.id")
    @Mapping(target = "ticketCode", source = "ticket.ticketCode")
    @Mapping(target = "orderItems", source = "orderItems")
    SubOrderResponse toResponse(SubOrderEntity entity);

    List<SubOrderResponse> toListResponse(List<SubOrderEntity> entities);
}
