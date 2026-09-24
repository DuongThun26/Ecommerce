package com.example.ecommerce.order.mapper;

import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.order.dto.request.OrderRequest;
import com.example.ecommerce.order.entity.OrderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {
    // Map request sang entity nhưng bỏ qua các trường sẽ được set ở service.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "rejectedReason", ignore = true)
    @Mapping(target = "returnedReason", ignore = true)
    @Mapping(target = "reviewNotes", ignore = true)
    @Mapping(target = "ticket", ignore = true)
    OrderEntity toEntity(OrderRequest request);

    // Map entity sang response, lấy thêm customerId từ quan hệ customer.
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "approvedById", source = "approvedBy.id")
    @Mapping(target = "approvedByUsername", source = "approvedBy.username")
    @Mapping(target = "ticketId", source = "ticket.id")
    @Mapping(target = "ticketCode", source = "ticket.ticketCode")
    OrderResponse toResponse(OrderEntity order);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "approvedById", source = "approvedBy.id")
    @Mapping(target = "approvedByUsername", source = "approvedBy.username")
    @Mapping(target = "ticketId", source = "ticket.id")
    @Mapping(target = "ticketCode", source = "ticket.ticketCode")
    List<OrderResponse> toListResponse(List<OrderEntity> orders);
}
