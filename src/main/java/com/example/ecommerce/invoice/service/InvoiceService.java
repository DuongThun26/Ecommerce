package com.example.ecommerce.invoice.service;

import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.invoice.dto.response.InvoiceResponse;
import com.example.ecommerce.invoice.entity.InvoiceEntity;
import com.example.ecommerce.order.entity.OrderEntity;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Map;

public interface InvoiceService {

    InvoiceResponse getInvoiceById(Long invoiceId, String username, List<String> roles);

    InvoiceResponse getInvoiceByOrderId(Long orderId, String username, List<String> roles);

    InvoiceResponse getInvoiceByInvoiceNumber(String invoiceNumber, String username, List<String> roles);

    PageResponse<InvoiceResponse> getInvoices(Map<String, Object> params, List<String> sortByParams);

    PageResponse<InvoiceResponse> getInvoicesByCustomerId(Long customerId, Map<String, Object> params, List<String> sortByParams, String username, List<String> roles);

    InvoiceResponse createInvoiceForOrder(OrderEntity order);

    InvoiceResponse createInvoiceForOrderId(Long orderId);

    byte[] getInvoicePdfContent(Long invoiceId, String username, List<String> roles);

    Resource getInvoicePdfResource(Long invoiceId, String username, List<String> roles);

    void generateAndStorePdf(Long invoiceId);
}
