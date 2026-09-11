package com.example.ecommerce.mapper;

import com.example.ecommerce.dto.request.ProductRequest;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.dto.response.ProductResponseSummary;
import com.example.ecommerce.entity.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(target = "id", ignore = true)
    ProductEntity toEntity(ProductRequest request);
    ProductResponse toResponse(ProductEntity entity);
    ProductResponseSummary toResponseSummary(ProductEntity entity);
    List<ProductResponseSummary> toResponseSummaryList(List<ProductEntity> entities);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(ProductRequest request, @MappingTarget ProductEntity entity);
}
