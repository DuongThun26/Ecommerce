package com.example.ecommerce.controller;

import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.invoice.controller.CustomerInvoiceController;
import com.example.ecommerce.invoice.controller.InvoiceController;
import com.example.ecommerce.invoice.controller.OrderInvoiceController;
import com.example.ecommerce.invoice.dto.response.InvoiceItemResponse;
import com.example.ecommerce.invoice.dto.response.InvoiceResponse;
import com.example.ecommerce.invoice.enums.InvoiceStatus;
import com.example.ecommerce.invoice.service.InvoiceService;
import com.example.ecommerce.payment.enums.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({InvoiceController.class, OrderInvoiceController.class, CustomerInvoiceController.class})
@AutoConfigureMockMvc(addFilters = false)
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvoiceService invoiceService;

    @MockitoBean
    private com.example.ecommerce.customer.repository.CustomerRepository customerRepository;

    @Test
    @DisplayName("GET /invoices should return paged invoices")
    void getInvoices_ShouldReturnPagedResponse() throws Exception {
        InvoiceResponse response = InvoiceResponse.builder()
                .id(1L)
                .invoiceNumber("INV-20260924-000001")
                .orderId(10L)
                .customerId(5L)
                .status(InvoiceStatus.ISSUED)
                .totalAmount(BigDecimal.valueOf(1000000))
                .build();

        PageResponse<InvoiceResponse> pageResponse = PageResponse.<InvoiceResponse>builder()
                .content(List.of(response))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .isLast(true)
                .build();

        when(invoiceService.getInvoices(anyMap(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/invoices")
                        .param("status", "ISSUED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].invoiceNumber").value("INV-20260924-000001"));
    }

    @Test
    @DisplayName("GET /invoices/{id} should return invoice details")
    void getInvoiceById_ShouldReturnInvoiceDetails() throws Exception {
        InvoiceItemResponse item = InvoiceItemResponse.builder()
                .id(100L)
                .productId(20L)
                .productName("Sản phẩm mẫu")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(500000))
                .subtotal(BigDecimal.valueOf(1000000))
                .build();

        InvoiceResponse response = InvoiceResponse.builder()
                .id(1L)
                .invoiceNumber("INV-20260924-000001")
                .orderId(10L)
                .customerId(5L)
                .billingName("Nguyen Van A")
                .billingEmail("vana@example.com")
                .status(InvoiceStatus.ISSUED)
                .totalAmount(BigDecimal.valueOf(1000000))
                .paymentMethod(PaymentMethod.VNPAY)
                .issueDate(LocalDateTime.now())
                .items(List.of(item))
                .build();

        when(invoiceService.getInvoiceById(eq(1L), any(), anyList())).thenReturn(response);

        mockMvc.perform(get("/invoices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.billingName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.data.items[0].productName").value("Sản phẩm mẫu"));
    }

    @Test
    @DisplayName("GET /invoices/{id}/pdf should stream PDF attachment")
    void downloadInvoicePdf_ShouldReturnPdfStream() throws Exception {
        InvoiceResponse response = InvoiceResponse.builder()
                .id(1L)
                .invoiceNumber("INV-20260924-000001")
                .build();

        byte[] pdfContent = "%PDF-1.4 Mock PDF Content".getBytes();
        Resource resource = new ByteArrayResource(pdfContent);

        when(invoiceService.getInvoiceById(eq(1L), any(), anyList())).thenReturn(response);
        when(invoiceService.getInvoicePdfResource(eq(1L), any(), anyList())).thenReturn(resource);

        mockMvc.perform(get("/invoices/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"INV-20260924-000001.pdf\""))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfContent));
    }

    @Test
    @DisplayName("GET /orders/{orderId}/invoice should return invoice for order")
    void getInvoiceByOrderId_ShouldReturnInvoice() throws Exception {
        InvoiceResponse response = InvoiceResponse.builder()
                .id(1L)
                .invoiceNumber("INV-20260924-000001")
                .orderId(10L)
                .build();

        when(invoiceService.getInvoiceByOrderId(eq(10L), any(), anyList())).thenReturn(response);

        mockMvc.perform(get("/orders/10/invoice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(10));
    }

    @Test
    @DisplayName("GET /customers/{customerId}/invoices should return customer invoices")
    void getCustomerInvoices_ShouldReturnCustomerInvoices() throws Exception {
        InvoiceResponse response = InvoiceResponse.builder()
                .id(1L)
                .customerId(5L)
                .invoiceNumber("INV-20260924-000001")
                .build();

        PageResponse<InvoiceResponse> pageResponse = PageResponse.<InvoiceResponse>builder()
                .content(List.of(response))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .isLast(true)
                .build();

        when(invoiceService.getInvoicesByCustomerId(eq(5L), anyMap(), any(), any(), anyList())).thenReturn(pageResponse);

        mockMvc.perform(get("/customers/5/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].customerId").value(5));
    }

    @Test
    @DisplayName("POST /invoices/orders/{orderId}/generate should create invoice")
    void generateInvoiceForOrder_ShouldReturnCreated() throws Exception {
        InvoiceResponse response = InvoiceResponse.builder()
                .id(1L)
                .invoiceNumber("INV-20260924-000001")
                .orderId(10L)
                .status(InvoiceStatus.ISSUED)
                .build();

        when(invoiceService.createInvoiceForOrderId(eq(10L))).thenReturn(response);

        mockMvc.perform(post("/invoices/orders/10/generate"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.invoiceNumber").value("INV-20260924-000001"));
    }
}
