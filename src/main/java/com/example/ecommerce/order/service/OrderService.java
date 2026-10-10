package com.example.ecommerce.order.service;

import com.example.ecommerce.order.dto.request.OrderRequest;
import com.example.ecommerce.order.dto.response.OrderStatusHistoryResponse;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.order.enums.OrderStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface OrderService {
    OrderResponse getOrderById(Long orderId);
    OrderResponse createOrder(OrderRequest request);
    OrderResponse createOrder(OrderRequest request, String username);
    OrderResponse createDraftOrder(OrderRequest request);
    OrderResponse createDraftOrder(OrderRequest request, String username);
    OrderResponse submitOrder(Long orderId, String actorType, String actorIdentifier);
    OrderResponse approveOrder(Long orderId, String poUsername, String notes);
    OrderResponse rejectOrder(Long orderId, String poUsername, String reason);
    OrderResponse returnOrder(Long orderId, String poUsername, String reason);
    OrderResponse updateReturnedOrder(Long orderId, com.example.ecommerce.order.dto.request.OrderUpdateRequest request, String actorType, String actorIdentifier);

    OrderResponse handleCustomerDecision(Long orderId, com.example.ecommerce.order.dto.request.CustomerDecisionRequest request, String customerUsername);
    OrderResponse approveSubOrder(Long subOrderId, String poUsername, String notes);
    OrderResponse rejectSubOrder(Long subOrderId, String poUsername, String reason);
    OrderResponse returnSubOrder(Long subOrderId, String poUsername, String reason);
    com.example.ecommerce.order.dto.response.SubOrderResponse getSubOrderById(Long subOrderId);

    PageResponse<OrderResponse> getOrders(Map<String, Object> params, List<String> sortByParams);

    default PageResponse<OrderResponse> getOrders(Map<String, Object> params) {
        return getOrders(params, null);
    }

    PageResponse<OrderResponse> getOrdersByCustomerId(Long customerId, Map<String, Object> params, List<String> sortByParams);

    default PageResponse<OrderResponse> getOrdersByCustomerId(Long customerId, Map<String, Object> params) {
        return getOrdersByCustomerId(customerId, params, null);
    }

    OrderResponse updateOrderStatus(Long orderId, OrderStatus status);
    OrderResponse updateOrderStatus(Long orderId, OrderStatus status, String actorType, String actorIdentifier, String reason);
    OrderResponse cancelOrder(Long orderId, String actorType, String actorIdentifier, String reason);
    List<OrderStatusHistoryResponse> getOrderStatusHistory(Long orderId);
    void cancelExpiredOrders();

    com.example.ecommerce.order.dto.response.OrderItemsResponse addOrderItem(Long orderId, com.example.ecommerce.order.dto.request.AddOrderItemRequest request);
    com.example.ecommerce.order.dto.response.OrderItemsResponse updateOrderItemQuantity(Long orderId, Long productId, com.example.ecommerce.order.dto.request.UpdateOrderItemRequest request);
    com.example.ecommerce.order.dto.response.OrderItemsResponse deleteOrderItem(Long orderId, Long productId);
}
