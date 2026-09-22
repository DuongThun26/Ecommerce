package com.example.ecommerce.payment.mapper;

import com.example.ecommerce.payment.dto.response.PaymentResponse;
import com.example.ecommerce.payment.entity.PaymentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "orderId", source = "order.id")
    PaymentResponse toResponse(PaymentEntity payment);
}
