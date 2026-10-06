package com.example.ecommerce.order.mapper;

import com.example.ecommerce.order.dto.request.OrderItemRequest;
import com.example.ecommerce.order.dto.response.OrderItemResponse;
import com.example.ecommerce.order.entity.OrderItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderItemMapper {
    // Chỉ map các field người dùng nhập, còn quan hệ và giá sẽ được set trong service.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "subTotal", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "product", ignore = true)
    OrderItemEntity toEntity(OrderItemRequest request);

    // Khi trả dữ liệu ra ngoài, lấy productId và thông tin category từ product.
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "categoryId", source = "product.category.id")
    @Mapping(target = "categoryName", source = "product.category.name")
    @Mapping(target = "categoryCode", source = "product.category.code")
    @Mapping(target = "managedByUserId", source = "product.category.managedBy.id")
    @Mapping(target = "managedByUsername", source = "product.category.managedBy.username")
    OrderItemResponse toResponse(OrderItemEntity orderItem);
}
