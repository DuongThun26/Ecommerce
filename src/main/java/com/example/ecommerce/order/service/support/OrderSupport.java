package com.example.ecommerce.order.service.support;

import com.example.ecommerce.auth.entity.UserEntity;
import com.example.ecommerce.auth.enums.UserRole;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.customer.enums.CustomerStatus;
import com.example.ecommerce.customer.repository.CustomerRepository;
import com.example.ecommerce.order.dto.request.OrderRequest;
import com.example.ecommerce.order.dto.response.OrderItemDetailResponse;
import com.example.ecommerce.order.dto.response.OrderItemsResponse;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.OrderItemEntity;
import com.example.ecommerce.order.entity.OrderStatusHistoryEntity;
import com.example.ecommerce.order.entity.SubOrderEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.enums.SubOrderStatus;
import com.example.ecommerce.order.repository.OrderStatusHistoryRepository;
import com.example.ecommerce.payment.entity.PaymentEntity;
import com.example.ecommerce.payment.enums.PaymentStatus;
import com.example.ecommerce.payment.repository.PaymentRepository;
import com.example.ecommerce.product.entity.ProductEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderSupport {

    public static final String SYSTEM_ACTOR = "SYSTEM";

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final PaymentRepository paymentRepository;

    public void recordHistory(OrderEntity order, OrderStatus fromStatus, OrderStatus toStatus, String actorType, String actorIdentifier, String reason) {
        OrderStatusHistoryEntity history = OrderStatusHistoryEntity.builder()
                .order(order)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .actorType(actorType)
                .actorIdentifier(actorIdentifier)
                .reason(trimToNull(reason))
                .build();
        orderStatusHistoryRepository.save(history);
    }

    public CustomerEntity resolveCustomerForOrder(OrderRequest request, String username) {
        CustomerEntity customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findByIdAndDeletedFalse(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + request.getCustomerId() + " not found."));
        } else if (username != null && !username.isBlank()) {
            customer = customerRepository.findByUserUsername(username).orElse(null);
            if (customer == null) {
                UserEntity user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User " + username + " not found."));
                customer = customerRepository.save(CustomerEntity.builder()
                        .user(user)
                        .name(user.getUsername())
                        .email(user.getEmail() != null ? user.getEmail() : user.getUsername() + "@customer.local")
                        .phone("0000000000")
                        .status(CustomerStatus.ACTIVE)
                        .deleted(false)
                        .build());
            }
        }
        if (customer == null) {
            throw new BadRequestException("Không tìm thấy thông tin khách hàng tương ứng với tài khoản đang đăng nhập.");
        }
        return customer;
    }

    public UserEntity validateAndGetPoUser(String poUsername) {
        if (poUsername == null || poUsername.isBlank()) {
            throw new BadRequestException("Username người phê duyệt không được để trống.");
        }
        return userRepository.findByUsername(poUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User " + poUsername + " not found."));
    }

    public void validatePoOwnershipForSubOrder(SubOrderEntity subOrder, UserEntity poUser) {
        if (poUser.getRole() == UserRole.ADMIN) {
            return;
        }
        if (subOrder.getManagedBy() != null && !subOrder.getManagedBy().getId().equals(poUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xử lý đơn hàng con #" + subOrder.getSubOrderCode() + " vì do PO '" + subOrder.getManagedBy().getUsername() + "' quản lý.");
        }
    }

    public void validateCustomerOwnershipForMasterOrder(OrderEntity masterOrder, String customerUsername) {
        if (customerUsername == null || customerUsername.isBlank()) {
            return;
        }
        UserEntity user = userRepository.findByUsername(customerUsername).orElse(null);
        if (user == null || user.getRole() == UserRole.ADMIN) {
            return; // Admin có toàn quyền
        }
        if (masterOrder.getCustomer() != null && masterOrder.getCustomer().getUser() != null) {
            if (!masterOrder.getCustomer().getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("Bạn chỉ có quyền đưa ra quyết định trên đơn hàng của chính mình.");
            }
        }
    }

    public void restoreStock(OrderEntity order) {
        if (order.getOrderItems() == null) return;
        for (OrderItemEntity orderItem : order.getOrderItems()) {
            ProductEntity product = orderItem.getProduct();
            if (product != null) {
                product.setStock(product.getStock() + orderItem.getQuantity());
            }
        }
    }

    public void restoreStockForSubOrder(SubOrderEntity subOrder) {
        if (subOrder.getOrderItems() == null) return;
        for (OrderItemEntity item : subOrder.getOrderItems()) {
            ProductEntity product = item.getProduct();
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
            }
        }
    }

    public void updatePendingPaymentStatus(OrderEntity order, PaymentStatus paymentStatus) {
        PaymentEntity payment = resolvePayment(order);
        if (payment != null && payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(paymentStatus);
            paymentRepository.save(payment);
        }
    }

    public PaymentEntity resolvePayment(OrderEntity order) {
        if (order.getPayments() != null && !order.getPayments().isEmpty()) {
            return order.getPayments().get(0);
        }
        return paymentRepository.findByOrderId(order.getId()).orElse(null);
    }

    public void recalculateOrderTotal(OrderEntity order) {
        BigDecimal total = BigDecimal.ZERO;
        if (order.getOrderItems() != null) {
            for (OrderItemEntity item : order.getOrderItems()) {
                total = total.add(item.getSubTotal());
            }
        }
        order.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
    }

    public void recalculateSubOrderTotal(SubOrderEntity subOrder) {
        BigDecimal total = BigDecimal.ZERO;
        if (subOrder.getOrderItems() != null) {
            for (OrderItemEntity item : subOrder.getOrderItems()) {
                total = total.add(item.getSubTotal());
            }
        }
        subOrder.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
    }

    public OrderItemEntity findOrderItemByProductId(OrderEntity order, Long productId) {
        if (order.getOrderItems() == null) return null;
        for (OrderItemEntity it : order.getOrderItems()) {
            if (it.getProduct() != null && Objects.equals(it.getProduct().getId(), productId)) {
                return it;
            }
        }
        return null;
    }

    public SubOrderEntity findSubOrderByPo(OrderEntity order, UserEntity poUser) {
        if (order.getSubOrders() == null) return null;
        for (SubOrderEntity so : order.getSubOrders()) {
            if (Objects.equals(so.getManagedBy(), poUser)) {
                return so;
            }
        }
        return null;
    }

    public SubOrderEntity findPendingSubOrderByPo(OrderEntity order, UserEntity poUser) {
        if (order.getSubOrders() == null) return null;
        for (SubOrderEntity so : order.getSubOrders()) {
            if (Objects.equals(so.getManagedBy(), poUser) && so.getStatus() == SubOrderStatus.PENDING_APPROVAL) {
                return so;
            }
        }
        return null;
    }

    public OrderItemEntity findOrderItemInSubOrder(SubOrderEntity subOrder, Long productId) {
        if (subOrder == null || subOrder.getOrderItems() == null) return null;
        for (OrderItemEntity it : subOrder.getOrderItems()) {
            if (it.getProduct() != null && Objects.equals(it.getProduct().getId(), productId)) {
                return it;
            }
        }
        return null;
    }

    public OrderItemsResponse toOrderItemsResponse(OrderEntity order) {
        List<OrderItemDetailResponse> details = new ArrayList<>();
        if (order.getOrderItems() != null) {
            for (OrderItemEntity item : order.getOrderItems()) {
                ProductEntity product = item.getProduct();
                BigDecimal unitPrice = item.getUnitPrice();
                BigDecimal subTotal = item.getSubTotal();
                if (unitPrice != null && unitPrice.scale() > 0 && unitPrice.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
                    unitPrice = unitPrice.setScale(0);
                }
                if (subTotal != null && subTotal.scale() > 0 && subTotal.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
                    subTotal = subTotal.setScale(0);
                }
                details.add(OrderItemDetailResponse.builder()
                        .productId(product != null ? product.getId() : null)
                        .productName(product != null ? product.getName() : null)
                        .unitPrice(unitPrice)
                        .quantity(item.getQuantity())
                        .subtotal(subTotal)
                        .build());
            }
        }
        BigDecimal totalAmount = order.getTotalAmount();
        if (totalAmount != null && totalAmount.scale() > 0 && totalAmount.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            totalAmount = totalAmount.setScale(0);
        }
        return OrderItemsResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus() != null ? order.getStatus().name() : OrderStatus.PENDING_APPROVAL.name())
                .items(details)
                .totalAmount(totalAmount)
                .build();
    }

    public String generateMasterOrderCode(Long orderId) {
        return "MO-" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now()) + "-" + String.format("%05d", orderId);
    }

    public String generateSubOrderCode(Long masterOrderId, int subIndex) {
        return "SO-" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now()) + "-" + String.format("%05d", masterOrderId) + "-P" + subIndex;
    }

    public String generateTicketCodeForSubOrder(Long subOrderId) {
        return "TK-SUB-" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now()) + "-" + String.format("%05d", subOrderId);
    }

    public String generateTransactionRef(Long orderId) {
        return "ORD" + orderId + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public String normalizeActorType(String actorType) {
        if (actorType == null || actorType.isBlank()) {
            return SYSTEM_ACTOR;
        }
        return actorType.trim().toUpperCase(Locale.ROOT);
    }

    public String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
