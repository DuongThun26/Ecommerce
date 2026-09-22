package com.example.ecommerce.controller;

import com.example.ecommerce.order.dto.request.OrderItemRequest;
import com.example.ecommerce.order.dto.request.OrderRequest;
import com.example.ecommerce.order.dto.request.OrderStatusRequest;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.controller.OrderController;
import com.example.ecommerce.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("POST /orders should return wrapped order")
    void createOrderShouldReturnWrappedOrder() throws Exception {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(2);

        OrderRequest request = new OrderRequest();
        request.setCustomerId(1L);
        request.setOrderItems(List.of(item));

        OrderResponse response = OrderResponse.builder().id(100L).customerId(1L).status(OrderStatus.PENDING_PAYMENT).build();
        when(orderService.createOrder(any(OrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /orders/{id} should return wrapped order")
    void getOrderByIdShouldReturnWrappedOrder() throws Exception {
        OrderResponse response = OrderResponse.builder().id(200L).customerId(2L).status(OrderStatus.CONFIRMED).build();
        when(orderService.getOrderById(200L)).thenReturn(response);

        mockMvc.perform(get("/orders/200"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /orders/{id}/status should return wrapped updated order")
    void updateOrderStatusShouldReturnWrappedOrder() throws Exception {
        OrderStatusRequest request = new OrderStatusRequest();
        request.setStatus(OrderStatus.COMPLETED);
        OrderResponse response = OrderResponse.builder().id(1L).status(OrderStatus.COMPLETED).customerId(1L).build();
        when(orderService.updateOrderStatus(1L, OrderStatus.COMPLETED)).thenReturn(response);

        mockMvc.perform(patch("/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /orders should return wrapped page response")
    void getOrdersShouldReturnWrappedPage() throws Exception {
        PageResponse<OrderResponse> page = PageResponse.<OrderResponse>builder()
                .content(List.of(OrderResponse.builder().id(1L).customerId(1L).status(OrderStatus.PENDING_PAYMENT).build()))
                .pageNumber(0)
                .pageSize(20)
                .totalElements(1)
                .totalPages(1)
                .isLast(true)
                .build();
        when(orderService.getOrders(any(), any())).thenReturn(page);

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk());
    }
}
