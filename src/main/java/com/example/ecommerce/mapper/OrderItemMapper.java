package com.example.ecommerce.mapper;

import com.example.ecommerce.dto.request.OrderItemRequest;
import com.example.ecommerce.dto.response.OrderItemResponse;
import com.example.ecommerce.entity.OrderItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {
    // Chỉ map các field người dùng nhập, còn quan hệ và giá sẽ được set trong service.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "subTotal", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "product", ignore = true)
    OrderItemEntity toEntity(OrderItemRequest request);

    // Khi trả dữ liệu ra ngoài, lấy productId từ object product.
    @Mapping(target = "productId", source = "product.id")
    OrderItemResponse toResponse(OrderItemEntity orderItem);
}
