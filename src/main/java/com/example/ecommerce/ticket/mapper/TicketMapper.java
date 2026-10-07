package com.example.ecommerce.ticket.mapper;

import com.example.ecommerce.order.mapper.OrderItemMapper;
import com.example.ecommerce.order.mapper.OrderMapper;
import com.example.ecommerce.order.mapper.SubOrderMapper;
import com.example.ecommerce.ticket.dto.response.TicketDetailResponse;
import com.example.ecommerce.ticket.dto.response.TicketResponse;
import com.example.ecommerce.ticket.entity.PurchaseTicketEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {OrderMapper.class, SubOrderMapper.class, OrderItemMapper.class})
public interface TicketMapper {

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "customerEmail", source = "customer.email")
    @Mapping(target = "totalAmount", source = "order.totalAmount")
    @Mapping(target = "reviewerId", source = "reviewer.id")
    @Mapping(target = "reviewerUsername", source = "reviewer.username")
    @Mapping(target = "subOrderId", source = "subOrder.id")
    @Mapping(target = "subOrderCode", source = "subOrder.subOrderCode")
    @Mapping(target = "managedByUserId", source = "subOrder.managedBy.id")
    @Mapping(target = "managedByUsername", source = "subOrder.managedBy.username")
    @Mapping(target = "orderItems", source = "subOrder.orderItems")
    TicketResponse toResponse(PurchaseTicketEntity ticket);

    List<TicketResponse> toListResponse(List<PurchaseTicketEntity> tickets);

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "customerEmail", source = "customer.email")
    @Mapping(target = "customerPhone", source = "customer.phone")
    @Mapping(target = "totalAmount", source = "order.totalAmount")
    @Mapping(target = "reviewerId", source = "reviewer.id")
    @Mapping(target = "reviewerUsername", source = "reviewer.username")
    @Mapping(target = "orderDetails", source = "order")
    @Mapping(target = "subOrderId", source = "subOrder.id")
    @Mapping(target = "subOrderCode", source = "subOrder.subOrderCode")
    @Mapping(target = "managedByUserId", source = "subOrder.managedBy.id")
    @Mapping(target = "managedByUsername", source = "subOrder.managedBy.username")
    @Mapping(target = "subOrderDetails", source = "subOrder")
    @Mapping(target = "orderItems", source = "subOrder.orderItems")
    TicketDetailResponse toDetailResponse(PurchaseTicketEntity ticket);

    @AfterMapping
    default void enrichTicketResponse(@MappingTarget TicketResponse response, PurchaseTicketEntity entity) {
        if (response != null && entity != null) {
            if (entity.getSubOrder() != null) {
                if (entity.getSubOrder().getTotalAmount() != null) {
                    response.setTotalAmount(entity.getSubOrder().getTotalAmount());
                }
                if (entity.getSubOrder().getStatus() == com.example.ecommerce.order.enums.SubOrderStatus.CANCELLED) {
                    response.setStatus(com.example.ecommerce.ticket.enums.TicketStatus.CANCELLED);
                }
            }
            if (entity.getOrder() != null && entity.getOrder().getStatus() == com.example.ecommerce.order.enums.OrderStatus.CANCELLED) {
                response.setStatus(com.example.ecommerce.ticket.enums.TicketStatus.CANCELLED);
            }
        }
    }

    @AfterMapping
    default void enrichTicketDetailResponse(@MappingTarget TicketDetailResponse response, PurchaseTicketEntity entity) {
        if (response != null && entity != null) {
            if (entity.getSubOrder() != null) {
                if (entity.getSubOrder().getTotalAmount() != null) {
                    response.setTotalAmount(entity.getSubOrder().getTotalAmount());
                }
                if (entity.getSubOrder().getStatus() == com.example.ecommerce.order.enums.SubOrderStatus.CANCELLED) {
                    response.setStatus(com.example.ecommerce.ticket.enums.TicketStatus.CANCELLED);
                }
            }
            if (entity.getOrder() != null && entity.getOrder().getStatus() == com.example.ecommerce.order.enums.OrderStatus.CANCELLED) {
                response.setStatus(com.example.ecommerce.ticket.enums.TicketStatus.CANCELLED);
            }
        }
    }
}

