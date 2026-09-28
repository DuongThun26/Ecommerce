package com.example.ecommerce.controller;

import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.customer.controller.CustomerOrderController;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class CustomerOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private com.example.ecommerce.customer.repository.CustomerRepository customerRepository;

    @Test
    @DisplayName("GET /customers/{customerId}/orders should return wrapped page response")
    void getCustomerOrdersShouldReturnWrappedPage() throws Exception {
        PageResponse<OrderResponse> page = PageResponse.<OrderResponse>builder()
                .content(List.of(OrderResponse.builder().id(1L).customerId(10L).status(OrderStatus.PAYMENT_PENDING).build()))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .isLast(true)
                .build();
        when(orderService.getOrdersByCustomerId(org.mockito.ArgumentMatchers.eq(10L), anyMap(), anyList())).thenReturn(page);

        mockMvc.perform(get("/customers/10/orders")
                .param("status", "PAYMENT_PENDING")
                        .param("createdFrom", "2026-09-01")
                        .param("createdTo", "2026-09-30")
                        .param("minTotalAmount", "100000")
                        .param("maxTotalAmount", "500000")
                        .param("keyword", "Nguyen")
                        .param("sortBy", "totalAmount,asc"))
                .andExpect(status().isOk());
    }
}

