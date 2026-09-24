package com.example.ecommerce.invoice.mapper;

import com.example.ecommerce.invoice.dto.response.InvoiceItemResponse;
import com.example.ecommerce.invoice.dto.response.InvoiceResponse;
import com.example.ecommerce.invoice.dto.response.InvoiceSummaryResponse;
import com.example.ecommerce.invoice.entity.InvoiceEntity;
import com.example.ecommerce.invoice.entity.InvoiceItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "items", source = "items")
    InvoiceResponse toResponse(InvoiceEntity invoice);

    InvoiceItemResponse toItemResponse(InvoiceItemEntity item);

    List<InvoiceItemResponse> toItemResponseList(List<InvoiceItemEntity> items);

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "customerId", source = "customer.id")
    InvoiceSummaryResponse toSummaryResponse(InvoiceEntity invoice);

    List<InvoiceSummaryResponse> toSummaryResponseList(List<InvoiceEntity> invoices);
}
