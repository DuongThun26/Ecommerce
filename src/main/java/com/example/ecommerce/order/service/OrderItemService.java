package com.example.ecommerce.order.service;

import com.example.ecommerce.order.dto.request.OrderItemRequest;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.OrderItemEntity;
import com.example.ecommerce.product.entity.ProductEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public interface OrderItemService {
    Map<Long, Integer> mergeRequestedQuantities(List<OrderItemRequest> orderItems);
    Map<Long, ProductEntity> validateAndLoadProducts(Map<Long, Integer> requestedQuantities);
    List<OrderItemEntity> createOrderItems(OrderEntity order, Map<Long, ProductEntity> productById, Map<Long, Integer> requestedQuantities);
    BigDecimal calculateTotalAmount(List<OrderItemEntity> orderItems);
    void decrementStock(Map<Long, ProductEntity> productById, Map<Long, Integer> requestedQuantities);
}
