package com.example.ecommerce.order.service.usecase;

import com.example.ecommerce.common.exception.OrderProcessingException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.order.dto.request.CustomerDecisionRequest;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.SubOrderEntity;
import com.example.ecommerce.order.enums.CustomerDecisionAction;
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
import com.example.ecommerce.ticket.entity.PurchaseTicketEntity;
import com.example.ecommerce.ticket.enums.TicketStatus;
import com.example.ecommerce.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerDecisionUseCase {

    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;
    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final OrderMapper orderMapper;
    private final OrderSupport orderSupport;

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse handleCustomerDecision(Long orderId, CustomerDecisionRequest request, String customerUsername) {
        OrderEntity masterOrder = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Master order with id " + orderId + " not found."));

        if (masterOrder.getStatus() != OrderStatus.ACTION_REQUIRED) {
            throw new OrderProcessingException("Chỉ đơn hàng ở trạng thái ACTION_REQUIRED mới có thể đưa ra quyết định. Trạng thái hiện tại: " + masterOrder.getStatus());
        }

        orderSupport.validateCustomerOwnershipForMasterOrder(masterOrder, customerUsername);

        LocalDateTime now = LocalDateTime.now();
        if (request.getAction() == CustomerDecisionAction.ACCEPT_PARTIAL) {
            // 1. Restore stock for rejected/returned sub-orders and mark them CANCELLED
            List<SubOrderEntity> subOrders = subOrderRepository.findByMasterOrderId(masterOrder.getId());
            for (SubOrderEntity subOrder : subOrders) {
                if (subOrder.getStatus() == SubOrderStatus.REJECTED || subOrder.getStatus() == SubOrderStatus.RETURNED) {
                    orderSupport.restoreStockForSubOrder(subOrder);
                    subOrder.setStatus(SubOrderStatus.CANCELLED);
                    subOrder.setStatusChangedAt(now);
                    subOrder.setRejectedReason("Khách hàng chỉ tiếp tục các đơn con được duyệt.");
                    subOrderRepository.save(subOrder);

                    ticketRepository.findBySubOrderId(subOrder.getId()).ifPresent(ticket -> {
                        ticket.setStatus(TicketStatus.CANCELLED);
                        ticket.setRejectionReason("Khách hàng chỉ tiếp tục các đơn hàng con được duyệt.");
                        ticket.setReviewedAt(now);
                        ticketRepository.save(ticket);
                    });
                }
            }

            // 2. Calculate new total amount for approved sub-orders
            BigDecimal newTotalAmount = BigDecimal.ZERO;
            int approvedCount = 0;
            for (SubOrderEntity subOrder : subOrders) {
                if (subOrder.getStatus() == SubOrderStatus.APPROVED) {
                    newTotalAmount = newTotalAmount.add(subOrder.getTotalAmount());
                    approvedCount++;
                }
            }

            if (approvedCount == 0 || newTotalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new OrderProcessingException("Không có đơn hàng con nào được phê duyệt để tiếp tục. Vui lòng chọn hủy toàn bộ đơn hàng.");
            }

            // 3. Update master order to AWAITING_PAYMENT
            masterOrder.setTotalAmount(newTotalAmount);
            masterOrder.setStatus(OrderStatus.AWAITING_PAYMENT);
            masterOrder.setStatusChangedAt(now);
            masterOrder.setPaymentExpiresAt(now.plusHours(24));
            masterOrder.setCancelReason(null);

            // 4. Update or create Payment
            PaymentEntity payment = orderSupport.resolvePayment(masterOrder);
            if (payment == null) {
                payment = PaymentEntity.builder()
                        .totalAmount(newTotalAmount)
                        .order(masterOrder)
                        .paymentMethod(PaymentMethod.VNPAY)
                        .status(PaymentStatus.PENDING)
                        .transactionRef(orderSupport.generateTransactionRef(masterOrder.getId()))
                        .build();
            } else {
                payment.setStatus(PaymentStatus.PENDING);
                payment.setTotalAmount(newTotalAmount);
                payment.setTransactionRef(orderSupport.generateTransactionRef(masterOrder.getId()));
            }
            paymentRepository.save(payment);

            orderSupport.recordHistory(masterOrder, OrderStatus.ACTION_REQUIRED, OrderStatus.AWAITING_PAYMENT, "CUSTOMER", customerUsername,
                    "Khách hàng đồng ý tiếp tục các đơn hàng con được duyệt (Tổng tiền mới: " + newTotalAmount + ") và loại bỏ các đơn bị từ chối.");

        } else if (request.getAction() == CustomerDecisionAction.CANCEL_ALL) {
            String cancelNote = "Khách hàng hủy toàn bộ đơn hàng do có sản phẩm bị từ chối" + (request.getNotes() != null ? " (" + request.getNotes() + ")" : "");

            // 1. Restore stock for ALL active sub-orders
            List<SubOrderEntity> subOrders = subOrderRepository.findByMasterOrderId(masterOrder.getId());
            for (SubOrderEntity subOrder : subOrders) {
                if (subOrder.getStatus() != SubOrderStatus.CANCELLED) {
                    orderSupport.restoreStockForSubOrder(subOrder);
                    subOrder.setStatus(SubOrderStatus.CANCELLED);
                    subOrder.setStatusChangedAt(now);
                    subOrder.setRejectedReason(cancelNote);
                    subOrderRepository.save(subOrder);
                }
            }

            // 2. Cancel all tickets
            List<PurchaseTicketEntity> tickets = ticketRepository.findByOrderId(masterOrder.getId());
            for (PurchaseTicketEntity ticket : tickets) {
                if (ticket.getStatus() != TicketStatus.CANCELLED) {
                    ticket.setStatus(TicketStatus.CANCELLED);
                    ticket.setRejectionReason(cancelNote);
                    ticket.setReviewedAt(now);
                    ticketRepository.save(ticket);
                }
            }

            // 3. Update master order to CANCELLED
            masterOrder.setStatus(OrderStatus.CANCELLED);
            masterOrder.setStatusChangedAt(now);
            masterOrder.setCancelReason(cancelNote);

            orderSupport.updatePendingPaymentStatus(masterOrder, PaymentStatus.CANCELLED);

            orderSupport.recordHistory(masterOrder, OrderStatus.ACTION_REQUIRED, OrderStatus.CANCELLED, "CUSTOMER", customerUsername, cancelNote);
        }

        return orderMapper.toResponse(orderRepository.save(masterOrder));
    }
}
