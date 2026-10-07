package com.example.ecommerce.order.mapper;

import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.order.dto.request.OrderRequest;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.SubOrderEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.enums.SubOrderStatus;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class, SubOrderMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {
    // Map request sang entity nhưng bỏ qua các trường sẽ được set ở service.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderCode", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    @Mapping(target = "subOrders", ignore = true)
    @Mapping(target = "tickets", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "rejectedReason", ignore = true)
    @Mapping(target = "returnedReason", ignore = true)
    @Mapping(target = "reviewNotes", ignore = true)
    OrderEntity toEntity(OrderRequest request);

    // Map entity sang response, lấy thêm customerId và customerName từ quan hệ customer.
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "approvedById", source = "approvedBy.id")
    @Mapping(target = "approvedByUsername", source = "approvedBy.username")
    @Mapping(target = "subOrders", source = "subOrders")
    OrderResponse toResponse(OrderEntity order);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "approvedById", source = "approvedBy.id")
    @Mapping(target = "approvedByUsername", source = "approvedBy.username")
    @Mapping(target = "subOrders", source = "subOrders")
    List<OrderResponse> toListResponse(List<OrderEntity> orders);

    @AfterMapping
    default void enrichOrderResponse(@MappingTarget OrderResponse response, OrderEntity entity) {
        if (response == null || entity == null) {
            return;
        }
        response.setCanMakeDecision(entity.getStatus() == OrderStatus.ACTION_REQUIRED);
        if (entity.getSubOrders() != null) {
            int approved = 0;
            int rejected = 0;
            int pending = 0;
            int returned = 0;
            for (SubOrderEntity sub : entity.getSubOrders()) {
                if (sub.getStatus() == SubOrderStatus.APPROVED) approved++;
                else if (sub.getStatus() == SubOrderStatus.REJECTED) rejected++;
                else if (sub.getStatus() == SubOrderStatus.PENDING_APPROVAL) pending++;
                else if (sub.getStatus() == SubOrderStatus.RETURNED) returned++;
            }
            response.setApprovedSubOrdersCount(approved);
            response.setRejectedSubOrdersCount(rejected);
            response.setPendingSubOrdersCount(pending);
            response.setReturnedSubOrdersCount(returned);
        }
        if (entity.getTickets() != null && !entity.getTickets().isEmpty()) {
            response.setTicketId(entity.getTickets().get(0).getId());
            response.setTicketCode(entity.getTickets().get(0).getTicketCode());
        }
    }
}

