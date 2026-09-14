package com.example.ecommerce.mapper;

import com.example.ecommerce.dto.response.OrderResponse;
import com.example.ecommerce.entity.OrderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface OrderMapper {
    // Map request sang entity nhưng bỏ qua các trường sẽ được set ở service.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    OrderEntity toEntity(com.example.ecommerce.dto.request.OrderRequest request);

    // Map entity sang response, lấy thêm customerId từ quan hệ customer.
    @Mapping(target = "customerId", source = "customer.id")
    OrderResponse toResponse(OrderEntity order);
}
