package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.OrderRequest;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.OrderResponse;
import com.example.ecommerce.enums.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface OrderService {
    OrderResponse getOrderById(Long orderId);
    OrderResponse createOrder(OrderRequest request);
    PageResponse<OrderResponse> getOrders(OrderStatus status, Pageable pageable);
    PageResponse<OrderResponse> getOrdersByCustomerId(Long customerId, OrderStatus status, Pageable pageable);
    OrderResponse updateOrderStatus(Long orderId, OrderStatus status);
    void cancelExpiredOrders();
}
