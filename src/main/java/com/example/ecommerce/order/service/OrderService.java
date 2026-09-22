package com.example.ecommerce.order.service;

import com.example.ecommerce.order.dto.request.OrderRequest;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.order.enums.OrderStatus;
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
