package com.example.ecommerce.order.service.usecase;

import com.example.ecommerce.auth.entity.UserEntity;
import com.example.ecommerce.auth.enums.UserRole;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.category.entity.CategoryEntity;
import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.common.exception.OrderProcessingException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.OrderItemEntity;
import com.example.ecommerce.order.entity.SubOrderEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.enums.SubOrderStatus;
import com.example.ecommerce.order.mapper.OrderMapper;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.order.repository.SubOrderRepository;
import com.example.ecommerce.order.service.support.OrderSupport;
import com.example.ecommerce.payment.entity.PaymentEntity;
import com.example.ecommerce.payment.enums.PaymentMethod;
import com.example.ecommerce.payment.enums.PaymentStatus;
import com.example.ecommerce.payment.repository.PaymentRepository;
import com.example.ecommerce.product.entity.ProductEntity;
import com.example.ecommerce.ticket.enums.TicketStatus;
import com.example.ecommerce.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubOrderReviewUseCase {

    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;
    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final OrderSupport orderSupport;

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse approveSubOrder(Long subOrderId, String poUsername, String notes) {
        SubOrderEntity subOrder = subOrderRepository.findByIdForUpdate(subOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-order with id " + subOrderId + " not found."));

        if (subOrder.getStatus() != SubOrderStatus.PENDING_APPROVAL) {
            throw new OrderProcessingException("Only PENDING_APPROVAL sub-orders can be approved. Current status: " + subOrder.getStatus());
        }

        UserEntity poUser = orderSupport.validateAndGetPoUser(poUsername);
        orderSupport.validatePoOwnershipForSubOrder(subOrder, poUser);

        LocalDateTime now = LocalDateTime.now();
        subOrder.setStatus(SubOrderStatus.APPROVED);
        subOrder.setStatusChangedAt(now);
        subOrder.setApprovedBy(poUser);
        subOrder.setApprovedAt(now);
        subOrder.setReviewNotes(orderSupport.trimToNull(notes));
        subOrder.setRejectedReason(null);
        subOrder.setReturnedReason(null);
        subOrderRepository.save(subOrder);

        // Update SubOrder's Ticket
        ticketRepository.findBySubOrderId(subOrder.getId()).ifPresent(ticket -> {
            ticket.setStatus(TicketStatus.APPROVED);
            ticket.setReviewer(poUser);
            ticket.setReviewedAt(now);
            ticket.setReviewNotes(orderSupport.trimToNull(notes));
            ticketRepository.save(ticket);
        });

        OrderEntity masterOrder = orderRepository.findByIdForUpdate(subOrder.getMasterOrder().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Master order not found"));

        evaluateAndSyncMasterOrderStatus(masterOrder, poUsername, "PO approved sub-order #" + subOrder.getSubOrderCode());
        return orderMapper.toResponse(orderRepository.save(masterOrder));
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse rejectSubOrder(Long subOrderId, String poUsername, String reason) {
        SubOrderEntity subOrder = subOrderRepository.findByIdForUpdate(subOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-order with id " + subOrderId + " not found."));

        if (subOrder.getStatus() != SubOrderStatus.PENDING_APPROVAL) {
            throw new OrderProcessingException("Only PENDING_APPROVAL sub-orders can be rejected. Current status: " + subOrder.getStatus());
        }

        String validReason = orderSupport.trimToNull(reason);
        if (validReason == null) {
            throw new BadRequestException("Lý do từ chối không được để trống.");
        }

        UserEntity poUser = orderSupport.validateAndGetPoUser(poUsername);
        orderSupport.validatePoOwnershipForSubOrder(subOrder, poUser);

        LocalDateTime now = LocalDateTime.now();
        subOrder.setStatus(SubOrderStatus.REJECTED);
        subOrder.setStatusChangedAt(now);
        subOrder.setRejectedReason(validReason);
        subOrder.setReturnedReason(null);
        subOrderRepository.save(subOrder);

        // Update SubOrder's Ticket
        ticketRepository.findBySubOrderId(subOrder.getId()).ifPresent(ticket -> {
            ticket.setStatus(TicketStatus.REJECTED);
            ticket.setReviewer(poUser);
            ticket.setReviewedAt(now);
            ticket.setRejectionReason(validReason);
            ticketRepository.save(ticket);
        });

        OrderEntity masterOrder = orderRepository.findByIdForUpdate(subOrder.getMasterOrder().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Master order not found"));

        evaluateAndSyncMasterOrderStatus(masterOrder, poUsername, "PO rejected sub-order #" + subOrder.getSubOrderCode() + ": " + validReason);
        return orderMapper.toResponse(orderRepository.save(masterOrder));
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse returnSubOrder(Long subOrderId, String poUsername, String reason) {
        SubOrderEntity subOrder = subOrderRepository.findByIdForUpdate(subOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-order with id " + subOrderId + " not found."));

        if (subOrder.getStatus() != SubOrderStatus.PENDING_APPROVAL) {
            throw new OrderProcessingException("Only PENDING_APPROVAL sub-orders can be returned. Current status: " + subOrder.getStatus());
        }

        String validReason = orderSupport.trimToNull(reason);
        if (validReason == null) {
            throw new BadRequestException("Lý do trả lại không được để trống.");
        }

        UserEntity poUser = orderSupport.validateAndGetPoUser(poUsername);
        orderSupport.validatePoOwnershipForSubOrder(subOrder, poUser);

        LocalDateTime now = LocalDateTime.now();
        subOrder.setStatus(SubOrderStatus.RETURNED);
        subOrder.setStatusChangedAt(now);
        subOrder.setReturnedReason(validReason);
        subOrderRepository.save(subOrder);

        // Update SubOrder's Ticket
        ticketRepository.findBySubOrderId(subOrder.getId()).ifPresent(ticket -> {
            ticket.setStatus(TicketStatus.RETURNED);
            ticket.setReviewer(poUser);
            ticket.setReviewedAt(now);
            ticket.setReturnedReason(validReason);
            ticketRepository.save(ticket);
        });

        OrderEntity masterOrder = orderRepository.findByIdForUpdate(subOrder.getMasterOrder().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Master order not found"));

        evaluateAndSyncMasterOrderStatus(masterOrder, poUsername, "PO returned sub-order #" + subOrder.getSubOrderCode() + ": " + validReason);
        return orderMapper.toResponse(orderRepository.save(masterOrder));
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse approveOrder(Long orderId, String poUsername, String notes) {
        OrderEntity masterOrder = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));

        UserEntity poUser = orderSupport.validateAndGetPoUser(poUsername);

        List<SubOrderEntity> subOrders = subOrderRepository.findByMasterOrderId(masterOrder.getId());
        if (subOrders.isEmpty()) {
            validatePoCategoryOwnershipForOrder(masterOrder, poUsername);
            masterOrder.setStatus(OrderStatus.AWAITING_PAYMENT);
            masterOrder.setStatusChangedAt(LocalDateTime.now());
            masterOrder.setPaymentExpiresAt(LocalDateTime.now().plusHours(24));
            masterOrder.setApprovedBy(poUser);
            masterOrder.setApprovedAt(LocalDateTime.now());
            masterOrder.setReviewNotes(orderSupport.trimToNull(notes));
            return orderMapper.toResponse(orderRepository.save(masterOrder));
        }

        boolean approvedAny = false;
        for (SubOrderEntity subOrder : subOrders) {
            if (poUser.getRole() == UserRole.ADMIN || (subOrder.getManagedBy() != null && subOrder.getManagedBy().getId().equals(poUser.getId()))) {
                if (subOrder.getStatus() == SubOrderStatus.PENDING_APPROVAL) {
                    approveSubOrder(subOrder.getId(), poUsername, notes);
                    approvedAny = true;
                }
            }
        }

        if (!approvedAny && poUser.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Bạn không có đơn hàng con nào thuộc quyền quản lý trong đơn hàng #" + orderId);
        }

        return orderMapper.toResponse(orderRepository.findById(orderId).orElseThrow());
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse rejectOrder(Long orderId, String poUsername, String reason) {
        OrderEntity masterOrder = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));

        UserEntity poUser = orderSupport.validateAndGetPoUser(poUsername);

        List<SubOrderEntity> subOrders = subOrderRepository.findByMasterOrderId(masterOrder.getId());
        if (subOrders.isEmpty()) {
            validatePoCategoryOwnershipForOrder(masterOrder, poUsername);
            orderSupport.restoreStock(masterOrder);
            masterOrder.setStatus(OrderStatus.REJECTED);
            masterOrder.setStatusChangedAt(LocalDateTime.now());
            masterOrder.setRejectedReason(orderSupport.trimToNull(reason));
            return orderMapper.toResponse(orderRepository.save(masterOrder));
        }

        boolean rejectedAny = false;
        for (SubOrderEntity subOrder : subOrders) {
            if (poUser.getRole() == UserRole.ADMIN || (subOrder.getManagedBy() != null && subOrder.getManagedBy().getId().equals(poUser.getId()))) {
                if (subOrder.getStatus() == SubOrderStatus.PENDING_APPROVAL) {
                    rejectSubOrder(subOrder.getId(), poUsername, reason);
                    rejectedAny = true;
                }
            }
        }

        if (!rejectedAny && poUser.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Bạn không có đơn hàng con nào thuộc quyền quản lý trong đơn hàng #" + orderId);
        }

        return orderMapper.toResponse(orderRepository.findById(orderId).orElseThrow());
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse returnOrder(Long orderId, String poUsername, String reason) {
        OrderEntity masterOrder = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));

        UserEntity poUser = orderSupport.validateAndGetPoUser(poUsername);

        List<SubOrderEntity> subOrders = subOrderRepository.findByMasterOrderId(masterOrder.getId());
        if (subOrders.isEmpty()) {
            validatePoCategoryOwnershipForOrder(masterOrder, poUsername);
            orderSupport.restoreStock(masterOrder);
            masterOrder.setStatus(OrderStatus.RETURNED);
            masterOrder.setStatusChangedAt(LocalDateTime.now());
            masterOrder.setReturnedReason(orderSupport.trimToNull(reason));
            return orderMapper.toResponse(orderRepository.save(masterOrder));
        }

        for (SubOrderEntity subOrder : subOrders) {
            if (poUser.getRole() == UserRole.ADMIN || (subOrder.getManagedBy() != null && subOrder.getManagedBy().getId().equals(poUser.getId()))) {
                if (subOrder.getStatus() == SubOrderStatus.PENDING_APPROVAL) {
                    returnSubOrder(subOrder.getId(), poUsername, reason);
                }
            }
        }

        return orderMapper.toResponse(orderRepository.findById(orderId).orElseThrow());
    }

    public void evaluateAndSyncMasterOrderStatus(OrderEntity masterOrder, String poUsername, String reason) {
        List<SubOrderEntity> subOrders = subOrderRepository.findByMasterOrderId(masterOrder.getId());
        if (subOrders.isEmpty()) {
            return;
        }

        int total = subOrders.size();
        int pendingCount = 0;
        int approvedCount = 0;
        int rejectedCount = 0;
        int returnedCount = 0;
        int cancelledCount = 0;

        for (SubOrderEntity sub : subOrders) {
            switch (sub.getStatus()) {
                case PENDING_APPROVAL -> pendingCount++;
                case APPROVED -> approvedCount++;
                case REJECTED -> rejectedCount++;
                case RETURNED -> returnedCount++;
                case CANCELLED -> cancelledCount++;
                default -> {}
            }
        }

        LocalDateTime now = LocalDateTime.now();
        OrderStatus currentStatus = masterOrder.getStatus();

        if (pendingCount > 0) {
            masterOrder.setStatus(OrderStatus.PENDING_PO_REVIEW);
            masterOrder.setStatusChangedAt(now);
            orderSupport.recordHistory(masterOrder, currentStatus, OrderStatus.PENDING_PO_REVIEW, "PRODUCT_OWNER", poUsername, reason + " (Đang chờ các PO còn lại)");
            return;
        }

        if (approvedCount == total) {
            masterOrder.setStatus(OrderStatus.AWAITING_PAYMENT);
            masterOrder.setStatusChangedAt(now);
            masterOrder.setPaymentExpiresAt(now.plusHours(24));

            PaymentEntity payment = orderSupport.resolvePayment(masterOrder);
            if (payment == null) {
                payment = PaymentEntity.builder()
                        .totalAmount(masterOrder.getTotalAmount())
                        .order(masterOrder)
                        .paymentMethod(PaymentMethod.VNPAY)
                        .status(PaymentStatus.PENDING)
                        .transactionRef(orderSupport.generateTransactionRef(masterOrder.getId()))
                        .build();
            } else {
                payment.setStatus(PaymentStatus.PENDING);
                payment.setTotalAmount(masterOrder.getTotalAmount());
            }
            paymentRepository.save(payment);

            orderSupport.recordHistory(masterOrder, currentStatus, OrderStatus.AWAITING_PAYMENT, "PRODUCT_OWNER", poUsername,
                    "Tất cả các PO đã phê duyệt toàn bộ đơn hàng. Đơn hàng chuyển sang chờ thanh toán.");

        } else if (rejectedCount == total) {
            orderSupport.restoreStock(masterOrder);
            masterOrder.setStatus(OrderStatus.REJECTED);
            masterOrder.setStatusChangedAt(now);
            masterOrder.setRejectedReason("Tất cả các Product Owner đã từ chối duyệt các sản phẩm trong đơn.");
            orderSupport.updatePendingPaymentStatus(masterOrder, PaymentStatus.CANCELLED);

            orderSupport.recordHistory(masterOrder, currentStatus, OrderStatus.REJECTED, "PRODUCT_OWNER", poUsername,
                    "Tất cả các Product Owner đã từ chối đơn hàng. Đơn hàng bị từ chối toàn bộ.");

        } else if (approvedCount > 0 && (rejectedCount > 0 || returnedCount > 0)) {
            masterOrder.setStatus(OrderStatus.ACTION_REQUIRED);
            masterOrder.setStatusChangedAt(now);

            orderSupport.recordHistory(masterOrder, currentStatus, OrderStatus.ACTION_REQUIRED, "PRODUCT_OWNER", poUsername,
                    "Đơn hàng có một số sản phẩm được duyệt và một số sản phẩm bị từ chối. Chờ khách hàng đưa ra quyết định.");
        }
    }

    public void validatePoCategoryOwnershipForOrder(OrderEntity order, String poUsername) {
        if (poUsername == null || poUsername.isBlank()) {
            return;
        }

        UserEntity user = userRepository.findByUsername(poUsername).orElse(null);
        if (user == null || user.getRole() == UserRole.ADMIN) {
            return;
        }

        if (user.getRole() == UserRole.PRODUCT_OWNER) {
            if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
                return;
            }

            for (OrderItemEntity item : order.getOrderItems()) {
                ProductEntity product = item.getProduct();
                if (product != null) {
                    CategoryEntity category = product.getCategory();
                    if (category == null || category.getManagedBy() == null || !user.getId().equals(category.getManagedBy().getId())) {
                        String catName = (category != null) ? category.getName() : "Chưa gắn loại hàng";
                        String poAssigned = (category != null && category.getManagedBy() != null) ? category.getManagedBy().getUsername() : "Chưa phân công";
                        throw new AccessDeniedException(String.format(
                                "Bạn không có quyền phê duyệt/xử lý đơn hàng #%d vì đơn hàng chứa sản phẩm '%s' thuộc loại hàng '%s' (do PO '%s' quản lý).",
                                order.getId(), product.getName(), catName, poAssigned
                        ));
                    }
                }
            }
        }
    }
}
