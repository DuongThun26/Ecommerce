package com.example.ecommerce.ticket.mapper;

import com.example.ecommerce.order.mapper.OrderMapper;
import com.example.ecommerce.ticket.dto.response.TicketDetailResponse;
import com.example.ecommerce.ticket.dto.response.TicketResponse;
import com.example.ecommerce.ticket.entity.PurchaseTicketEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {OrderMapper.class})
public interface TicketMapper {

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "customerEmail", source = "customer.email")
    @Mapping(target = "totalAmount", source = "order.totalAmount")
    @Mapping(target = "reviewerId", source = "reviewer.id")
    @Mapping(target = "reviewerUsername", source = "reviewer.username")
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
    TicketDetailResponse toDetailResponse(PurchaseTicketEntity ticket);
}
