package com.example.ecommerce.customer.mapper;

import com.example.ecommerce.customer.dto.request.CustomerRequest;
import com.example.ecommerce.customer.dto.response.CustomerResponse;
import com.example.ecommerce.customer.dto.response.CustomerResponseSummary;
import com.example.ecommerce.customer.entity.CustomerEntity;
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
