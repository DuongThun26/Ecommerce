package com.example.ecommerce.product.mapper;

import com.example.ecommerce.product.dto.request.ProductRequest;
import com.example.ecommerce.product.dto.response.ProductImageResponse;
import com.example.ecommerce.product.dto.response.ProductResponse;
import com.example.ecommerce.product.dto.response.ProductResponseSummary;
import com.example.ecommerce.product.entity.ProductEntity;
import com.example.ecommerce.product.entity.ProductImageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    ProductEntity toEntity(ProductRequest request);

    @Mapping(target = "primaryImageUrl", expression = "java(entity.getPrimaryImageUrl())")
    @Mapping(target = "imageUrls", expression = "java(entity.extractImageUrls())")
    @Mapping(target = "images", source = "images")
    ProductResponse toResponse(ProductEntity entity);

    @Mapping(target = "primaryImageUrl", expression = "java(entity.getPrimaryImageUrl())")
    @Mapping(target = "imageUrls", expression = "java(entity.extractImageUrls())")
    ProductResponseSummary toResponseSummary(ProductEntity entity);

    List<ProductResponseSummary> toResponseSummaryList(List<ProductEntity> entities);

    ProductImageResponse toImageResponse(ProductImageEntity imageEntity);
    List<ProductImageResponse> toImageResponseList(List<ProductImageEntity> imageEntities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(ProductRequest request, @MappingTarget ProductEntity entity);
}
