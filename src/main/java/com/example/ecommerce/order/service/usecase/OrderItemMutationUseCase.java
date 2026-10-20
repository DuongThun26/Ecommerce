package com.example.ecommerce.order.service.usecase;

import com.example.ecommerce.auth.entity.UserEntity;
import com.example.ecommerce.common.exception.InsufficientStockException;
import com.example.ecommerce.common.exception.OrderNotEditableException;
import com.example.ecommerce.common.exception.ProductInactiveException;
import com.example.ecommerce.common.exception.QuantityLimitExceededException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.common.exception.ValidationErrorException;
import com.example.ecommerce.order.dto.request.AddOrderItemRequest;
import com.example.ecommerce.order.dto.request.UpdateOrderItemRequest;
import com.example.ecommerce.order.dto.response.OrderItemsResponse;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.OrderItemEntity;
import com.example.ecommerce.order.entity.SubOrderEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.enums.SubOrderStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.order.repository.SubOrderRepository;
import com.example.ecommerce.order.service.support.OrderSupport;
import com.example.ecommerce.product.entity.ProductEntity;
import com.example.ecommerce.product.enums.ProductStatus;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.ticket.entity.PurchaseTicketEntity;
import com.example.ecommerce.ticket.enums.TicketStatus;
import com.example.ecommerce.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderItemMutationUseCase {

    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;
    private final ProductRepository productRepository;
    private final TicketRepository ticketRepository;
    private final OrderSupport orderSupport;
    private final SubOrderReviewUseCase subOrderReviewUseCase;

    @Transactional(rollbackFor = Exception.class)
    public OrderItemsResponse addOrderItem(Long orderId, AddOrderItemRequest request) {
        if (request == null || request.getProductId() == null || request.getQuantity() == null
                || request.getQuantity() < 1 || request.getQuantity() > 99) {
            throw new ValidationErrorException("quantity trong mỗi request PHẢI từ 1 đến 99.");
        }

        Long productId = request.getProductId();
        int requestedQuantity = request.getQuantity();

        OrderEntity order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));

        if (!isOrderEditable(order)) {
            throw new OrderNotEditableException("Chỉ cho phép thêm, sửa, xoá item khi đơn hàng ở trạng thái PENDING_APPROVAL hoặc DRAFT.");
        }

        ProductEntity product = productRepository.findByIdAndDeletedFalseForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + productId + " not found."));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ProductInactiveException("Sản phẩm không ở trạng thái ACTIVE.");
        }

        boolean isDraft = (order.getStatus() == OrderStatus.DRAFT);
        UserEntity poUser = (product.getCategory() != null) ? product.getCategory().getManagedBy() : null;

        if (isDraft) {
            OrderItemEntity existingItem = orderSupport.findOrderItemByProductId(order, productId);
            if (existingItem != null) {
                if (existingItem.getQuantity() + requestedQuantity > 99) {
                    throw new QuantityLimitExceededException("quantity của 1 item sau khi cộng KHÔNG ĐƯỢC vượt 99.");
                }
                int newQty = existingItem.getQuantity() + requestedQuantity;
                existingItem.setQuantity(newQty);
                existingItem.setSubTotal(existingItem.getUnitPrice().multiply(BigDecimal.valueOf(newQty)).setScale(2, RoundingMode.HALF_UP));
            } else {
                if (requestedQuantity > 99) {
                    throw new QuantityLimitExceededException("quantity của 1 item sau khi cộng KHÔNG ĐƯỢC vượt 99.");
                }
                BigDecimal unitPrice = product.getPrice();
                BigDecimal subTotal = unitPrice.multiply(BigDecimal.valueOf(requestedQuantity)).setScale(2, RoundingMode.HALF_UP);
                OrderItemEntity newItem = OrderItemEntity.builder()
                        .order(order)
                        .product(product)
                        .quantity(requestedQuantity)
                        .unitPrice(unitPrice)
                        .subTotal(subTotal)
                        .build();
                if (order.getOrderItems() == null) {
                    order.setOrderItems(new ArrayList<>());
                }
                order.getOrderItems().add(newItem);
            }
        } else {
            if (product.getStock() < requestedQuantity) {
                throw new InsufficientStockException("Không đủ tồn kho.");
            }
            product.setStock(product.getStock() - requestedQuantity);

            SubOrderEntity pendingSubOrder = orderSupport.findPendingSubOrderByPo(order, poUser);
            if (pendingSubOrder != null) {
                OrderItemEntity existingItem = orderSupport.findOrderItemInSubOrder(pendingSubOrder, productId);
                if (existingItem != null) {
                    if (existingItem.getQuantity() + requestedQuantity > 99) {
                        throw new QuantityLimitExceededException("quantity của 1 item sau khi cộng KHÔNG ĐƯỢC vượt 99.");
                    }
                    int newQty = existingItem.getQuantity() + requestedQuantity;
                    existingItem.setQuantity(newQty);
                    existingItem.setSubTotal(existingItem.getUnitPrice().multiply(BigDecimal.valueOf(newQty)).setScale(2, RoundingMode.HALF_UP));
                } else {
                    if (requestedQuantity > 99) {
                        throw new QuantityLimitExceededException("quantity của 1 item sau khi cộng KHÔNG ĐƯỢC vượt 99.");
                    }
                    BigDecimal unitPrice = product.getPrice();
                    BigDecimal subTotal = unitPrice.multiply(BigDecimal.valueOf(requestedQuantity)).setScale(2, RoundingMode.HALF_UP);
                    OrderItemEntity newItem = OrderItemEntity.builder()
                            .order(order)
                            .subOrder(pendingSubOrder)
                            .product(product)
                            .quantity(requestedQuantity)
                            .unitPrice(unitPrice)
                            .subTotal(subTotal)
                            .build();
                    if (order.getOrderItems() == null) {
                        order.setOrderItems(new ArrayList<>());
                    }
                    order.getOrderItems().add(newItem);
                    if (pendingSubOrder.getOrderItems() == null) {
                        pendingSubOrder.setOrderItems(new ArrayList<>());
                    }
                    pendingSubOrder.getOrderItems().add(newItem);
                }
                orderSupport.recalculateSubOrderTotal(pendingSubOrder);
            } else {
                if (requestedQuantity > 99) {
                    throw new QuantityLimitExceededException("quantity của 1 item sau khi cộng KHÔNG ĐƯỢC vượt 99.");
                }
                BigDecimal unitPrice = product.getPrice();
                BigDecimal subTotal = unitPrice.multiply(BigDecimal.valueOf(requestedQuantity)).setScale(2, RoundingMode.HALF_UP);

                int nextIndex = (order.getSubOrders() != null ? order.getSubOrders().size() : 0) + 1;
                String subOrderCode = orderSupport.generateSubOrderCode(order.getId(), nextIndex);

                SubOrderEntity newSubOrder = SubOrderEntity.builder()
                        .masterOrder(order)
                        .managedBy(poUser)
                        .subOrderCode(subOrderCode)
                        .status(SubOrderStatus.PENDING_APPROVAL)
                        .statusChangedAt(LocalDateTime.now())
                        .totalAmount(subTotal)
                        .orderItems(new ArrayList<>())
                        .build();
                SubOrderEntity savedSubOrder = subOrderRepository.save(newSubOrder);
                if (savedSubOrder != null) {
                    newSubOrder = savedSubOrder;
                }

                String ticketCode = orderSupport.generateTicketCodeForSubOrder(newSubOrder.getId() != null ? newSubOrder.getId() : System.currentTimeMillis());
                PurchaseTicketEntity ticket = PurchaseTicketEntity.builder()
                        .ticketCode(ticketCode)
                        .order(order)
                        .subOrder(newSubOrder)
                        .customer(order.getCustomer())
                        .reviewer(poUser)
                        .status(TicketStatus.PENDING_APPROVAL)
                        .title("Phiếu duyệt đơn con #" + subOrderCode + " của đơn hàng #" + order.getId())
                        .description("Phiếu mua hàng đang chờ Product Owner (" + (poUser != null ? poUser.getUsername() : "Chung") + ") phê duyệt.")
                        .deleted(false)
                        .build();
                ticketRepository.save(ticket);
                newSubOrder.setTicket(ticket);

                OrderItemEntity newItem = OrderItemEntity.builder()
                        .order(order)
                        .subOrder(newSubOrder)
                        .product(product)
                        .quantity(requestedQuantity)
                        .unitPrice(unitPrice)
                        .subTotal(subTotal)
                        .build();

                if (order.getOrderItems() == null) {
                    order.setOrderItems(new ArrayList<>());
                }
                order.getOrderItems().add(newItem);
                newSubOrder.getOrderItems().add(newItem);
                if (order.getSubOrders() == null) {
                    order.setSubOrders(new ArrayList<>());
                }
                order.getSubOrders().add(newSubOrder);
            }

            subOrderReviewUseCase.evaluateAndSyncMasterOrderStatus(order, "CUSTOMER", "Thêm đơn con/sản phẩm mới cho PO phê duyệt");
        }

        orderSupport.recalculateOrderTotal(order);
        order.setStatusChangedAt(LocalDateTime.now());
        orderRepository.save(order);

        return orderSupport.toOrderItemsResponse(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderItemsResponse updateOrderItemQuantity(Long orderId, Long productId, UpdateOrderItemRequest request) {
        if (request == null || request.getQuantity() == null
                || request.getQuantity() < 1 || request.getQuantity() > 99) {
            throw new ValidationErrorException("quantity trong mỗi request PHẢI từ 1 đến 99.");
        }

        int newQuantity = request.getQuantity();

        OrderEntity order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));

        if (!isOrderEditable(order)) {
            throw new OrderNotEditableException("Chỉ cho phép thêm, sửa, xoá item khi đơn hàng ở trạng thái PENDING_APPROVAL hoặc DRAFT.");
        }

        OrderItemEntity existingItem = null;
        if (order.getOrderItems() != null) {
            for (OrderItemEntity it : order.getOrderItems()) {
                if (it.getProduct() != null && it.getProduct().getId().equals(productId)) {
                    existingItem = it;
                    break;
                }
            }
        }
        if (existingItem == null) {
            throw new ResourceNotFoundException("Sản phẩm với id " + productId + " không có trong đơn hàng " + orderId);
        }

        ProductEntity product = productRepository.findByIdAndDeletedFalseForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + productId + " not found."));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ProductInactiveException("Sản phẩm không ở trạng thái ACTIVE.");
        }

        boolean isDraft = (order.getStatus() == OrderStatus.DRAFT);
        if (isDraft) {
            existingItem.setQuantity(newQuantity);
            existingItem.setSubTotal(existingItem.getUnitPrice().multiply(BigDecimal.valueOf(newQuantity)).setScale(2, RoundingMode.HALF_UP));
        } else {
            SubOrderEntity itemSubOrder = existingItem.getSubOrder();
            if (itemSubOrder != null && itemSubOrder.getStatus() == SubOrderStatus.APPROVED) {
                if (newQuantity < existingItem.getQuantity()) {
                    throw new OrderNotEditableException("Không thể giảm số lượng sản phẩm trong đơn con đã được phê duyệt.");
                }
                int delta = newQuantity - existingItem.getQuantity();
                if (delta > 0) {
                    if (product.getStock() < delta) {
                        throw new InsufficientStockException("Không đủ tồn kho.");
                    }
                    product.setStock(product.getStock() - delta);

                    UserEntity poUser = (product.getCategory() != null) ? product.getCategory().getManagedBy() : null;
                    int nextIndex = (order.getSubOrders() != null ? order.getSubOrders().size() : 0) + 1;
                    String subOrderCode = orderSupport.generateSubOrderCode(order.getId(), nextIndex);
                    BigDecimal subTotal = product.getPrice().multiply(BigDecimal.valueOf(delta)).setScale(2, RoundingMode.HALF_UP);

                    SubOrderEntity newSubOrder = SubOrderEntity.builder()
                            .masterOrder(order)
                            .managedBy(poUser)
                            .subOrderCode(subOrderCode)
                            .status(SubOrderStatus.PENDING_APPROVAL)
                            .statusChangedAt(LocalDateTime.now())
                            .totalAmount(subTotal)
                            .orderItems(new ArrayList<>())
                            .build();
                    newSubOrder = subOrderRepository.save(newSubOrder);

                    PurchaseTicketEntity ticket = PurchaseTicketEntity.builder()
                            .ticketCode(orderSupport.generateTicketCodeForSubOrder(newSubOrder.getId()))
                            .order(order)
                            .subOrder(newSubOrder)
                            .customer(order.getCustomer())
                            .reviewer(poUser)
                            .status(TicketStatus.PENDING_APPROVAL)
                            .title("Phiếu duyệt đơn con #" + subOrderCode + " của đơn hàng #" + order.getId())
                            .description("Phiếu mua hàng đang chờ Product Owner (" + (poUser != null ? poUser.getUsername() : "Chung") + ") phê duyệt.")
                            .deleted(false)
                            .build();
                    ticketRepository.save(ticket);
                    newSubOrder.setTicket(ticket);

                    OrderItemEntity newItem = OrderItemEntity.builder()
                            .order(order)
                            .subOrder(newSubOrder)
                            .product(product)
                            .quantity(delta)
                            .unitPrice(product.getPrice())
                            .subTotal(subTotal)
                            .build();

                    order.getOrderItems().add(newItem);
                    newSubOrder.getOrderItems().add(newItem);
                    if (order.getSubOrders() == null) {
                        order.setSubOrders(new ArrayList<>());
                    }
                    order.getSubOrders().add(newSubOrder);

                    subOrderReviewUseCase.evaluateAndSyncMasterOrderStatus(order, "CUSTOMER", "Thêm đơn con mới cho phần số lượng tăng thêm");
                }
            } else {
                int delta = newQuantity - existingItem.getQuantity();
                if (delta > 0) {
                    if (product.getStock() < delta) {
                        throw new InsufficientStockException("Không đủ tồn kho.");
                    }
                    product.setStock(product.getStock() - delta);
                } else if (delta < 0) {
                    product.setStock(product.getStock() + (-delta));
                }

                existingItem.setQuantity(newQuantity);
                existingItem.setSubTotal(existingItem.getUnitPrice().multiply(BigDecimal.valueOf(newQuantity)).setScale(2, RoundingMode.HALF_UP));

                if (existingItem.getSubOrder() != null) {
                    orderSupport.recalculateSubOrderTotal(existingItem.getSubOrder());
                }
            }
        }

        orderSupport.recalculateOrderTotal(order);
        order.setStatusChangedAt(LocalDateTime.now());
        orderRepository.save(order);

        return orderSupport.toOrderItemsResponse(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderItemsResponse deleteOrderItem(Long orderId, Long productId) {
        OrderEntity order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));

        if (!isOrderEditable(order)) {
            throw new OrderNotEditableException("Chỉ cho phép thêm, sửa, xoá item khi đơn hàng ở trạng thái PENDING_APPROVAL hoặc DRAFT.");
        }

        OrderItemEntity targetItem = null;
        if (order.getOrderItems() != null) {
            for (OrderItemEntity it : order.getOrderItems()) {
                if (it.getProduct() != null && it.getProduct().getId().equals(productId)) {
                    if (it.getSubOrder() == null || it.getSubOrder().getStatus() == SubOrderStatus.PENDING_APPROVAL) {
                        targetItem = it;
                        break;
                    }
                    if (targetItem == null) {
                        targetItem = it;
                    }
                }
            }
        }
        if (targetItem == null) {
            throw new ResourceNotFoundException("Sản phẩm với id " + productId + " không có trong đơn hàng " + orderId);
        }

        if (targetItem.getSubOrder() != null && targetItem.getSubOrder().getStatus() == SubOrderStatus.APPROVED) {
            throw new OrderNotEditableException("Không thể xóa sản phẩm trong đơn con đã được phê duyệt.");
        }

        ProductEntity product = productRepository.findByIdAndDeletedFalseForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + productId + " not found."));

        boolean isDraft = (order.getStatus() == OrderStatus.DRAFT);
        if (!isDraft) {
            product.setStock(product.getStock() + targetItem.getQuantity());
        }

        SubOrderEntity subOrder = targetItem.getSubOrder();
        if (subOrder != null && subOrder.getOrderItems() != null) {
            subOrder.getOrderItems().remove(targetItem);
            orderSupport.recalculateSubOrderTotal(subOrder);
        }

        order.getOrderItems().remove(targetItem);
        orderSupport.recalculateOrderTotal(order);
        order.setStatusChangedAt(LocalDateTime.now());
        orderRepository.save(order);

        return orderSupport.toOrderItemsResponse(order);
    }

    private boolean isOrderEditable(OrderEntity order) {
        return order.getStatus() == OrderStatus.PENDING_APPROVAL
                || order.getStatus() == OrderStatus.PENDING_PO_REVIEW
                || order.getStatus() == OrderStatus.DRAFT;
    }
}
