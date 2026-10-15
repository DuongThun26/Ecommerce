package com.example.ecommerce.customer.mapper;

import com.example.ecommerce.customer.dto.request.CustomerRequest;
import com.example.ecommerce.customer.dto.response.CustomerResponse;
import com.example.ecommerce.customer.dto.response.CustomerResponseSummary;
import com.example.ecommerce.customer.entity.CustomerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CustomerMapper {
    CustomerEntity toEntity(CustomerRequest request);

    @org.mapstruct.Mapping(source = "user.username", target = "username")
    @org.mapstruct.Mapping(source = "user.status", target = "userStatus")
    CustomerResponse toResponse(CustomerEntity entity);

    CustomerResponseSummary toResponseSummary(CustomerEntity entity);
    List<CustomerResponse> toResponseList(List<CustomerEntity> entities);
    void updateEntity(CustomerRequest request, @MappingTarget CustomerEntity entity);
}
