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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.example.ecommerce.order.dto.request.AddOrderItemRequest;
import com.example.ecommerce.order.dto.request.UpdateOrderItemRequest;
import com.example.ecommerce.order.dto.response.OrderItemsResponse;
import com.example.ecommerce.order.dto.response.OrderItemDetailResponse;
import java.math.BigDecimal;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
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

        OrderResponse response = OrderResponse.builder().id(100L).customerId(1L).status(OrderStatus.AWAITING_PAYMENT).build();
        when(orderService.createOrder(any(OrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET /orders/{id} should return wrapped order")
    void getOrderByIdShouldReturnWrappedOrder() throws Exception {
        OrderResponse response = OrderResponse.builder().id(200L).customerId(2L).status(OrderStatus.PAID).build();
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
        when(orderService.updateOrderStatus(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(OrderStatus.COMPLETED), any(), any(), any())).thenReturn(response);

        mockMvc.perform(patch("/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /orders should return wrapped page response")
    void getOrdersShouldReturnWrappedPage() throws Exception {
        PageResponse<OrderResponse> page = PageResponse.<OrderResponse>builder()
                .content(List.of(OrderResponse.builder().id(1L).customerId(1L).status(OrderStatus.AWAITING_PAYMENT).build()))
                .pageNumber(0)
                .pageSize(20)
                .totalElements(1)
                .totalPages(1)
                .isLast(true)
                .build();
        when(orderService.getOrders(anyMap(), anyList())).thenReturn(page);

        mockMvc.perform(get("/orders")
                        .param("status", "AWAITING_PAYMENT")
                        .param("keyword", "An")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "createdAt,desc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /orders/{orderId}/items should return 200 with OrderItemsResponse")
    void addOrderItemShouldReturnOrderItemsResponse() throws Exception {
        AddOrderItemRequest request = AddOrderItemRequest.builder().productId(10L).quantity(3).build();
        OrderItemsResponse response = OrderItemsResponse.builder()
                .orderId(100L)
                .status("PENDING")
                .items(List.of(OrderItemDetailResponse.builder()
                        .productId(10L)
                        .productName("iPhone 16 128GB")
                        .unitPrice(BigDecimal.valueOf(22990000))
                        .quantity(5)
                        .subtotal(BigDecimal.valueOf(114950000))
                        .build()))
                .totalAmount(BigDecimal.valueOf(114950000))
                .build();

        when(orderService.addOrderItem(org.mockito.ArgumentMatchers.eq(100L), any(AddOrderItemRequest.class))).thenReturn(response);

        mockMvc.perform(post("/orders/100/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items[0].productId").value(10))
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.totalAmount").value(114950000));
    }

    @Test
    @DisplayName("PATCH /orders/{orderId}/items/{productId} should return 200 with OrderItemsResponse")
    void updateOrderItemShouldReturnOrderItemsResponse() throws Exception {
        UpdateOrderItemRequest request = UpdateOrderItemRequest.builder().quantity(2).build();
        OrderItemsResponse response = OrderItemsResponse.builder()
                .orderId(100L)
                .status("PENDING")
                .items(List.of(OrderItemDetailResponse.builder()
                        .productId(10L)
                        .productName("iPhone 16 128GB")
                        .unitPrice(BigDecimal.valueOf(22990000))
                        .quantity(2)
                        .subtotal(BigDecimal.valueOf(45980000))
                        .build()))
                .totalAmount(BigDecimal.valueOf(45980000))
                .build();

        when(orderService.updateOrderItemQuantity(org.mockito.ArgumentMatchers.eq(100L), org.mockito.ArgumentMatchers.eq(10L), any(UpdateOrderItemRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/orders/100/items/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    @DisplayName("DELETE /orders/{orderId}/items/{productId} should return 200 with OrderItemsResponse")
    void deleteOrderItemShouldReturnOrderItemsResponse() throws Exception {
        OrderItemsResponse response = OrderItemsResponse.builder()
                .orderId(100L)
                .status("PENDING")
                .items(List.of())
                .totalAmount(BigDecimal.ZERO)
                .build();

        when(orderService.deleteOrderItem(100L, 10L)).thenReturn(response);

        mockMvc.perform(delete("/orders/100/items/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
