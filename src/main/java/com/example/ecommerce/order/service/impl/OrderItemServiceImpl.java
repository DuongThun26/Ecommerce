package com.example.ecommerce.order.service.impl;

import com.example.ecommerce.order.dto.request.OrderItemRequest;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.OrderItemEntity;
import com.example.ecommerce.product.entity.ProductEntity;
import com.example.ecommerce.product.enums.ProductStatus;
import com.example.ecommerce.common.exception.OrderProcessingException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.order.mapper.OrderItemMapper;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.order.service.OrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {

    private final ProductRepository productRepository;
    private final OrderItemMapper orderItemMapper;

    @Override
    public Map<Long, Integer> mergeRequestedQuantities(List<OrderItemRequest> orderItems) {
        // Gộp các dòng trùng productId để tính stock và subtotal chính xác.
        Map<Long, Integer> requestedQuantities = new LinkedHashMap<>();
        for (OrderItemRequest item : orderItems) {
            requestedQuantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return requestedQuantities;
    }

    @Override
    public Map<Long, ProductEntity> validateAndLoadProducts(Map<Long, Integer> requestedQuantities) {
        // Tải tất cả product một lần rồi kiểm tra tồn tại, trạng thái và stock.
        List<ProductEntity> products = productRepository.findByIdInAndDeletedFalse(requestedQuantities.keySet());
        Map<Long, ProductEntity> productById = new LinkedHashMap<>();
        for (ProductEntity product : products) {
            productById.put(product.getId(), product);
        }

        for (Long productId : requestedQuantities.keySet()) {
            ProductEntity product = productById.get(productId);
            if (product == null) {
                throw new ResourceNotFoundException("Product with id " + productId + " not found.");
            }
            if (product.getStatus() != ProductStatus.AVAILABLE) {
                throw new OrderProcessingException("Product with id " + productId + " is not active.");
            }
            int requestedQuantity = requestedQuantities.get(productId);
            if (product.getStock() < requestedQuantity) {
                throw new OrderProcessingException(
                        "Product with id " + productId + " does not have enough stock. Requested: "
                                + requestedQuantity + ", available: " + product.getStock()
                );
            }
        }

        return productById;
    }

    @Override
    public List<OrderItemEntity> createOrderItems(OrderEntity order, Map<Long, ProductEntity> productById, Map<Long, Integer> requestedQuantities) {
        // Tạo order item với unit price tại thời điểm mua để giữ đúng lịch sử đơn hàng.
        List<OrderItemEntity> orderItems = new ArrayList<>();
        for (Long productId : requestedQuantities.keySet()) {
            ProductEntity product = productById.get(productId);
            Integer quantity = requestedQuantities.get(productId);
            BigDecimal unitPrice = product.getPrice();
            BigDecimal subTotal = unitPrice
                    .multiply(BigDecimal.valueOf(quantity.longValue()))
                    .setScale(2, RoundingMode.HALF_UP);

            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(quantity);
            orderItem.setUnitPrice(unitPrice);
            orderItem.setSubTotal(subTotal);
            orderItems.add(orderItem);
        }
        return orderItems;
    }

    @Override
    public BigDecimal calculateTotalAmount(List<OrderItemEntity> orderItems) {
        // Tổng tiền của đơn hàng là tổng subtotal của tất cả item.
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItemEntity orderItem : orderItems) {
            totalAmount = totalAmount.add(orderItem.getSubTotal());
        }
        return totalAmount.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public void decrementStock(Map<Long, ProductEntity> productById, Map<Long, Integer> requestedQuantities) {
        // Trừ stock sau khi đã validate đủ số lượng để toàn bộ thay đổi nằm trong transaction.
        for (Long productId : requestedQuantities.keySet()) {
            ProductEntity product = productById.get(productId);
            Integer quantity = requestedQuantities.get(productId);
            product.setStock(product.getStock() - quantity);
        }
    }
}
