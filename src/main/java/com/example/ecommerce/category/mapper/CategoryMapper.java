package com.example.ecommerce.category.mapper;

import com.example.ecommerce.auth.dto.response.UserResponse;
import com.example.ecommerce.auth.entity.UserEntity;
import com.example.ecommerce.category.dto.request.CategoryRequest;
import com.example.ecommerce.category.dto.response.CategoryResponse;
import com.example.ecommerce.category.dto.response.CategorySummaryResponse;
import com.example.ecommerce.category.entity.CategoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "managedBy", ignore = true)
    @Mapping(target = "products", ignore = true)
    CategoryEntity toEntity(CategoryRequest request);

    @Mapping(target = "managedBy", source = "managedBy", qualifiedByName = "userToUserResponse")
    @Mapping(target = "productCount", expression = "java(entity.getProducts() != null ? entity.getProducts().size() : 0)")
    CategoryResponse toResponse(CategoryEntity entity);

    List<CategoryResponse> toResponseList(List<CategoryEntity> entities);

    @Mapping(target = "managedByUserId", source = "managedBy.id")
    @Mapping(target = "managedByUsername", source = "managedBy.username")
    CategorySummaryResponse toSummaryResponse(CategoryEntity entity);

    List<CategorySummaryResponse> toSummaryResponseList(List<CategoryEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "managedBy", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CategoryRequest request, @MappingTarget CategoryEntity entity);

    @Named("userToUserResponse")
    default UserResponse userToUserResponse(UserEntity user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }
}
