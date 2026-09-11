package com.example.ecommerce.mapper;

import com.example.ecommerce.dto.request.CustomerRequest;
import com.example.ecommerce.dto.response.CustomerResponse;
import com.example.ecommerce.dto.response.CustomerResponseSummary;
import com.example.ecommerce.entity.CustomerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CustomerMapper {
    CustomerEntity toEntity(CustomerRequest request);
    CustomerResponse toResponse(CustomerEntity entity);
    CustomerResponseSummary toResponseSummary(CustomerEntity entity);
    List<CustomerResponseSummary> toResponseSummaryList(List<CustomerEntity> entities);
    void updateEntity(CustomerRequest request, @MappingTarget CustomerEntity entity);
}
