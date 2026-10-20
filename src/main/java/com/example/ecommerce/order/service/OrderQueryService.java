package com.example.ecommerce.order.service;

import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.order.dto.response.OrderStatusHistoryResponse;
import com.example.ecommerce.order.dto.response.SubOrderResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface OrderQueryService {

    OrderResponse getOrderById(Long orderId);

    SubOrderResponse getSubOrderById(Long subOrderId);

    PageResponse<OrderResponse> getOrders(Map<String, Object> params, List<String> sortByParams);

    default PageResponse<OrderResponse> getOrders(Map<String, Object> params) {
        return getOrders(params, null);
    }

    PageResponse<OrderResponse> getOrdersByCustomerId(Long customerId, Map<String, Object> params, List<String> sortByParams);

    default PageResponse<OrderResponse> getOrdersByCustomerId(Long customerId, Map<String, Object> params) {
        return getOrdersByCustomerId(customerId, params, null);
    }

    List<OrderStatusHistoryResponse> getOrderStatusHistory(Long orderId);
}
