package com.example.ecommerce.order.service.impl;

import com.example.ecommerce.common.exception.OrderProcessingException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.customer.repository.CustomerRepository;
import com.example.ecommerce.invoice.enums.InvoiceStatus;
import com.example.ecommerce.invoice.repository.InvoiceRepository;
import com.example.ecommerce.invoice.service.InvoiceService;
import com.example.ecommerce.mail.service.EmailOutboxService;
import com.example.ecommerce.mail.service.EmailService;
import com.example.ecommerce.order.dto.request.AddOrderItemRequest;
import com.example.ecommerce.order.dto.request.CustomerDecisionRequest;
import com.example.ecommerce.order.dto.request.OrderRequest;
import com.example.ecommerce.order.dto.request.OrderUpdateRequest;
import com.example.ecommerce.order.dto.request.UpdateOrderItemRequest;
import com.example.ecommerce.order.dto.response.OrderItemDetailResponse;
import com.example.ecommerce.order.dto.response.OrderItemsResponse;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.order.dto.response.OrderStatusHistoryResponse;
import com.example.ecommerce.order.dto.response.SubOrderResponse;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.OrderItemEntity;
import com.example.ecommerce.order.entity.SubOrderEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.enums.SubOrderStatus;
import com.example.ecommerce.order.mapper.OrderMapper;
import com.example.ecommerce.order.mapper.SubOrderMapper;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.order.repository.OrderStatusHistoryRepository;
import com.example.ecommerce.order.repository.SubOrderRepository;
import com.example.ecommerce.order.service.OrderItemService;
import com.example.ecommerce.order.service.OrderQueryService;
import com.example.ecommerce.order.service.OrderService;
import com.example.ecommerce.order.service.support.OrderSupport;
import com.example.ecommerce.order.service.usecase.CreateOrderUseCase;
import com.example.ecommerce.order.service.usecase.CustomerDecisionUseCase;
import com.example.ecommerce.order.service.usecase.OrderItemMutationUseCase;
import com.example.ecommerce.order.service.usecase.SubOrderReviewUseCase;
import com.example.ecommerce.payment.entity.PaymentEntity;
import com.example.ecommerce.payment.enums.PaymentStatus;
import com.example.ecommerce.payment.repository.PaymentRepository;
import com.example.ecommerce.payment.vnpay.service.VnPayService;
import com.example.ecommerce.product.entity.ProductEntity;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.ticket.entity.PurchaseTicketEntity;
import com.example.ecommerce.ticket.enums.TicketStatus;
import com.example.ecommerce.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderItemService orderItemService;
    private final OrderMapper orderMapper;
    private final SubOrderMapper subOrderMapper;
    private final PaymentRepository paymentRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final com.example.ecommerce.auth.repository.UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;
    private final ObjectProvider<VnPayService> vnPayServiceProvider;
    private final EmailService emailService;
    private final EmailOutboxService emailOutboxService;

    // Extracted CQRS Query Service & Focused Use Cases
    private OrderSupport orderSupport;
    private OrderQueryService orderQueryService;
    private CreateOrderUseCase createOrderUseCase;
    private SubOrderReviewUseCase subOrderReviewUseCase;
    private CustomerDecisionUseCase customerDecisionUseCase;
    private OrderItemMutationUseCase orderItemMutationUseCase;

//    @Autowired
//    public OrderServiceImpl(
//            OrderRepository orderRepository,
//            SubOrderRepository subOrderRepository,
//            CustomerRepository customerRepository,
//            ProductRepository productRepository,
//            OrderItemService orderItemService,
//            OrderMapper orderMapper,
//            SubOrderMapper subOrderMapper,
//            PaymentRepository paymentRepository,
//            OrderStatusHistoryRepository orderStatusHistoryRepository,
//            com.example.ecommerce.auth.repository.UserRepository userRepository,
//            TicketRepository ticketRepository,
//            InvoiceService invoiceService,
//            InvoiceRepository invoiceRepository,
//            ObjectProvider<VnPayService> vnPayServiceProvider,
//            EmailService emailService,
//            EmailOutboxService emailOutboxService,
//            OrderSupport orderSupport,
//            OrderQueryService orderQueryService,
//            CreateOrderUseCase createOrderUseCase,
//            SubOrderReviewUseCase subOrderReviewUseCase,
//            CustomerDecisionUseCase customerDecisionUseCase,
//            OrderItemMutationUseCase orderItemMutationUseCase
//    ) {
//        this.orderRepository = orderRepository;
//        this.subOrderRepository = subOrderRepository;
//        this.customerRepository = customerRepository;
//        this.productRepository = productRepository;
//        this.orderItemService = orderItemService;
//        this.orderMapper = orderMapper;
//        this.subOrderMapper = subOrderMapper;
//        this.paymentRepository = paymentRepository;
//        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
//        this.userRepository = userRepository;
//        this.ticketRepository = ticketRepository;
//        this.invoiceService = invoiceService;
//        this.invoiceRepository = invoiceRepository;
//        this.vnPayServiceProvider = vnPayServiceProvider;
//        this.emailService = emailService;
//        this.emailOutboxService = emailOutboxService;
//        this.orderSupport = orderSupport;
//        this.orderQueryService = orderQueryService;
//        this.createOrderUseCase = createOrderUseCase;
//        this.subOrderReviewUseCase = subOrderReviewUseCase;
//        this.customerDecisionUseCase = customerDecisionUseCase;
//        this.orderItemMutationUseCase = orderItemMutationUseCase;
//    }
//
//    // Secondary constructor for Mockito unit test suites that instantiate with mock repositories
//    public OrderServiceImpl(
//            OrderRepository orderRepository,
//            SubOrderRepository subOrderRepository,
//            CustomerRepository customerRepository,
//            ProductRepository productRepository,
//            OrderItemService orderItemService,
//            OrderMapper orderMapper,
//            SubOrderMapper subOrderMapper,
//            PaymentRepository paymentRepository,
//            OrderStatusHistoryRepository orderStatusHistoryRepository,
//            com.example.ecommerce.auth.repository.UserRepository userRepository,
//            TicketRepository ticketRepository,
//            InvoiceService invoiceService,
//            InvoiceRepository invoiceRepository,
//            ObjectProvider<VnPayService> vnPayServiceProvider,
//            EmailService emailService,
//            EmailOutboxService emailOutboxService
//    ) {
//        this(
//                orderRepository, subOrderRepository, customerRepository, productRepository,
//                orderItemService, orderMapper, subOrderMapper, paymentRepository,
//                orderStatusHistoryRepository, userRepository, ticketRepository,
//                invoiceService, invoiceRepository, vnPayServiceProvider,
//                emailService, emailOutboxService,
//                null, null, null, null, null, null
//        );
//    }

    private OrderSupport getOrderSupport() {
        if (this.orderSupport == null) {
            this.orderSupport = new OrderSupport(customerRepository, userRepository, orderStatusHistoryRepository, paymentRepository);
        }
        return this.orderSupport;
    }

    private OrderQueryService getOrderQueryService() {
        if (this.orderQueryService == null) {
            this.orderQueryService = new OrderQueryServiceImpl(orderRepository, subOrderRepository, customerRepository, orderStatusHistoryRepository, orderMapper, subOrderMapper);
        }
        return this.orderQueryService;
    }

    private SubOrderReviewUseCase getSubOrderReviewUseCase() {
        if (this.subOrderReviewUseCase == null) {
            this.subOrderReviewUseCase = new SubOrderReviewUseCase(orderRepository, subOrderRepository, ticketRepository, paymentRepository, userRepository, orderMapper, getOrderSupport());
        }
        return this.subOrderReviewUseCase;
    }

    private CreateOrderUseCase getCreateOrderUseCase() {
        if (this.createOrderUseCase == null) {
            this.createOrderUseCase = new CreateOrderUseCase(orderRepository, subOrderRepository, orderItemService, ticketRepository, orderMapper, getOrderSupport(), getSubOrderReviewUseCase());
        }
        return this.createOrderUseCase;
    }

    private CustomerDecisionUseCase getCustomerDecisionUseCase() {
        if (this.customerDecisionUseCase == null) {
            this.customerDecisionUseCase = new CustomerDecisionUseCase(orderRepository, subOrderRepository, ticketRepository, paymentRepository, orderMapper, getOrderSupport());
        }
        return this.customerDecisionUseCase;
    }

    private OrderItemMutationUseCase getOrderItemMutationUseCase() {
        if (this.orderItemMutationUseCase == null) {
            this.orderItemMutationUseCase = new OrderItemMutationUseCase(orderRepository, subOrderRepository, productRepository, ticketRepository, getOrderSupport(), getSubOrderReviewUseCase());
        }
        return this.orderItemMutationUseCase;
    }

    // ==========================================
    // CQRS READ / QUERY METHODS (Delegated)
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        return getOrderQueryService().getOrderById(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public SubOrderResponse getSubOrderById(Long subOrderId) {
        return getOrderQueryService().getSubOrderById(subOrderId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrders(Map<String, Object> params) {
        return getOrderQueryService().getOrders(params);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrders(Map<String, Object> params, List<String> sortByParams) {
        return getOrderQueryService().getOrders(params, sortByParams);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrdersByCustomerId(Long customerId, Map<String, Object> params, List<String> sortByParams) {
        return getOrderQueryService().getOrdersByCustomerId(customerId, params, sortByParams);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getOrderStatusHistory(Long orderId) {
        return getOrderQueryService().getOrderStatusHistory(orderId);
    }

    // ==========================================
    // USE CASE 1: CreateOrderUseCase (Delegated)
    // ==========================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(OrderRequest request) {
        return getCreateOrderUseCase().createOrder(request, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(OrderRequest request, String username) {
        return getCreateOrderUseCase().createOrder(request, username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createDraftOrder(OrderRequest request) {
        return getCreateOrderUseCase().createDraftOrder(request, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createDraftOrder(OrderRequest request, String username) {
        return getCreateOrderUseCase().createDraftOrder(request, username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse submitOrder(Long orderId, String actorType, String actorIdentifier) {
        return getCreateOrderUseCase().submitOrder(orderId, actorType, actorIdentifier);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse updateReturnedOrder(Long orderId, OrderUpdateRequest request, String actorType, String actorIdentifier) {
        return getCreateOrderUseCase().updateReturnedOrder(orderId, request, actorType, actorIdentifier);
    }

    // ==========================================
    // USE CASE 2: SubOrderReviewUseCase (Delegated)
    // ==========================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse approveSubOrder(Long subOrderId, String poUsername, String notes) {
        return getSubOrderReviewUseCase().approveSubOrder(subOrderId, poUsername, notes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse rejectSubOrder(Long subOrderId, String poUsername, String reason) {
        return getSubOrderReviewUseCase().rejectSubOrder(subOrderId, poUsername, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse returnSubOrder(Long subOrderId, String poUsername, String reason) {
        return getSubOrderReviewUseCase().returnSubOrder(subOrderId, poUsername, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse approveOrder(Long orderId, String poUsername, String notes) {
        return getSubOrderReviewUseCase().approveOrder(orderId, poUsername, notes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse rejectOrder(Long orderId, String poUsername, String reason) {
        return getSubOrderReviewUseCase().rejectOrder(orderId, poUsername, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse returnOrder(Long orderId, String poUsername, String reason) {
        return getSubOrderReviewUseCase().returnOrder(orderId, poUsername, reason);
    }

    // ==========================================
    // USE CASE 3: CustomerDecisionUseCase (Delegated)
    // ==========================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse handleCustomerDecision(Long orderId, CustomerDecisionRequest request, String customerUsername) {
        return getCustomerDecisionUseCase().handleCustomerDecision(orderId, request, customerUsername);
    }

    // ==========================================
    // USE CASE 4: OrderItemMutationUseCase (Delegated)
    // ==========================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderItemsResponse addOrderItem(Long orderId, AddOrderItemRequest request) {
        return getOrderItemMutationUseCase().addOrderItem(orderId, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderItemsResponse updateOrderItemQuantity(Long orderId, Long productId, UpdateOrderItemRequest request) {
        return getOrderItemMutationUseCase().updateOrderItemQuantity(orderId, productId, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderItemsResponse deleteOrderItem(Long orderId, Long productId) {
        return getOrderItemMutationUseCase().deleteOrderItem(orderId, productId);
    }

    // ==========================================
    // ORDER LIFECYCLE & STATUS MANAGEMENT
    // ==========================================

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus status) {
        return updateOrderStatus(orderId, status, SYSTEM_ACTOR, null, null);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus status, String actorType, String actorIdentifier, String reason) {
        OrderEntity order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));

        if (status == OrderStatus.CANCELLED) {
            return cancelOrderInternal(order, actorType, actorIdentifier, reason);
        }

        if ("PRODUCT_OWNER".equalsIgnoreCase(actorType) || (actorIdentifier != null && !"ADMIN".equalsIgnoreCase(actorType) && !"SYSTEM".equalsIgnoreCase(actorType))) {
            getSubOrderReviewUseCase().validatePoCategoryOwnershipForOrder(order, actorIdentifier);
        }

        validateStatusTransition(order.getStatus(), status, actorType);
        OrderStatus previousStatus = order.getStatus();
        order.setStatus(status);
        order.setStatusChangedAt(LocalDateTime.now());
        if (status != OrderStatus.AWAITING_PAYMENT) {
            order.setCancelReason(null);
        }
        getOrderSupport().recordHistory(order, previousStatus, status, getOrderSupport().normalizeActorType(actorType), actorIdentifier, reason);
        OrderEntity savedOrder = orderRepository.save(order);

        // Sync Sub-orders status if advancing through lifecycle
        if (status == OrderStatus.PAID || status == OrderStatus.COMPLETED) {
            syncSubOrdersLifecycleStatus(savedOrder, status);
        }

        if (status == OrderStatus.PAID) {
            if (savedOrder.getPaidAt() == null) {
                savedOrder.setPaidAt(LocalDateTime.now());
                orderRepository.save(savedOrder);
            }
            try {
                invoiceService.createInvoiceForOrder(savedOrder);
            } catch (Exception ex) {
                log.error("Failed to automatically create invoice for paid order #{}", savedOrder.getId(), ex);
            }

            // REQ-03: Ghi 1 dòng EMAIL_OUTBOX (TEMPLATE = ORDER_PAID) trong cùng transaction.
            emailOutboxService.recordOrderPaidOutbox(savedOrder);
        }
        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId, String actorType, String actorIdentifier, String reason) {
        OrderEntity order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));
        return cancelOrderInternal(order, actorType, actorIdentifier, reason);
    }

    @Override
    @Transactional
    public void cancelExpiredOrders() {
        LocalDateTime cutOffTime = LocalDateTime.now();
        List<OrderEntity> expiredOrders = orderRepository.findByStatusAndPaymentExpiresAtBefore(OrderStatus.AWAITING_PAYMENT, cutOffTime);
        for (OrderEntity order : expiredOrders) {
            cancelExpiredOrder(order);
        }
    }

    private void syncSubOrdersLifecycleStatus(OrderEntity masterOrder, OrderStatus masterStatus) {
        List<SubOrderEntity> subOrders = subOrderRepository.findByMasterOrderId(masterOrder.getId());
        for (SubOrderEntity subOrder : subOrders) {
            if (subOrder.getStatus() == SubOrderStatus.APPROVED) {
                if (masterStatus == OrderStatus.COMPLETED) {
                    subOrder.setStatus(SubOrderStatus.COMPLETED);
                    subOrder.setStatusChangedAt(LocalDateTime.now());
                    subOrderRepository.save(subOrder);
                }
            }
        }
    }

    private void cancelExpiredOrder(OrderEntity order) {
        LocalDateTime now = LocalDateTime.now();
        getOrderSupport().restoreStock(order);
        getOrderSupport().updatePendingPaymentStatus(order, PaymentStatus.EXPIRED);

        List<SubOrderEntity> subOrders = subOrderRepository.findByMasterOrderId(order.getId());
        for (SubOrderEntity subOrder : subOrders) {
            subOrder.setStatus(SubOrderStatus.CANCELLED);
            subOrder.setStatusChangedAt(now);
            subOrder.setRejectedReason("Expired payment window");
            subOrderRepository.save(subOrder);
        }

        List<PurchaseTicketEntity> tickets = ticketRepository.findByOrderId(order.getId());
        for (PurchaseTicketEntity ticket : tickets) {
            if (ticket.getStatus() != TicketStatus.CANCELLED) {
                ticket.setStatus(TicketStatus.CANCELLED);
                ticket.setRejectionReason("Expired payment window");
                ticket.setReviewedAt(now);
                ticketRepository.save(ticket);
            }
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason("Expired payment window");
        order.setStatusChangedAt(now);

        getOrderSupport().recordHistory(order, previousStatus, OrderStatus.CANCELLED, SYSTEM_ACTOR, "scheduler", "Expired payment window");
        orderRepository.save(order);
    }

    private OrderResponse cancelOrderInternal(OrderEntity order, String actorType, String actorIdentifier, String reason) {
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderProcessingException("Không thể hủy đơn hàng ở trạng thái: " + order.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        String cancelReason = reason != null && !reason.isBlank() ? reason.trim() : "Khách hàng yêu cầu hủy đơn hàng";
        boolean wasPaid = (order.getStatus() == OrderStatus.PAID);

        // 1. Hoàn trả số lượng hàng tồn kho nếu không phải đơn nháp
        if (order.getStatus() != OrderStatus.DRAFT) {
            getOrderSupport().restoreStock(order);
        }

        PaymentEntity payment = getOrderSupport().resolvePayment(order);
        BigDecimal refundAmount = BigDecimal.ZERO;

        // 2. Xử lý hoàn tiền VNPay nếu đơn hàng đã thanh toán thành công
        if (payment != null) {
            if (wasPaid || payment.getStatus() == PaymentStatus.SUCCESS) {
                try {
                    refundAmount = payment.getTotalAmount();
                    vnPayServiceProvider.getObject().refund(payment, refundAmount, cancelReason, actorIdentifier != null ? actorIdentifier : actorType);
                } catch (Exception ex) {
                    log.error("Failed to execute VNPay refund for order #{}: {}", order.getId(), ex.getMessage(), ex);
                    payment.setStatus(PaymentStatus.REFUNDED);
                    payment.setRefundAmount(refundAmount);
                    payment.setRefundReason(cancelReason);
                    payment.setRefundedAt(now);
                    payment.setRefundTransactionNo("REF_MANUAL_" + System.currentTimeMillis());
                    paymentRepository.save(payment);
                }
            } else if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.CANCELLED);
                paymentRepository.save(payment);
            }
        }

        // 3. Hủy hóa đơn VAT điện tử nếu đã phát hành
        try {
            invoiceRepository.findByOrderId(order.getId()).ifPresent(invoice -> {
                invoice.setStatus(InvoiceStatus.CANCELLED);
                invoiceRepository.save(invoice);
                log.info("Cancelled invoice #{} for cancelled order #{}", invoice.getInvoiceNumber(), order.getId());
            });
        } catch (Exception ex) {
            log.error("Failed to cancel invoice for order #{}: {}", order.getId(), ex.getMessage());
        }

        // 4. Hủy tất cả đơn hàng con
        List<SubOrderEntity> subOrders = subOrderRepository.findByMasterOrderId(order.getId());
        for (SubOrderEntity subOrder : subOrders) {
            subOrder.setStatus(SubOrderStatus.CANCELLED);
            subOrder.setStatusChangedAt(now);
            subOrder.setRejectedReason(cancelReason);
            subOrderRepository.save(subOrder);
        }

        // 5. Hủy tất cả purchase tickets liên quan
        List<PurchaseTicketEntity> tickets = ticketRepository.findByOrderId(order.getId());
        for (PurchaseTicketEntity ticket : tickets) {
            if (ticket.getStatus() != TicketStatus.CANCELLED) {
                ticket.setStatus(TicketStatus.CANCELLED);
                ticket.setRejectionReason(cancelReason);
                ticket.setReviewedAt(now);
                ticketRepository.save(ticket);
            }
        }

        // 6. Cập nhật trạng thái Master Order & lưu lịch sử
        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(cancelReason);
        order.setStatusChangedAt(now);

        String historyReason = wasPaid
                ? cancelReason + " (Đã hoàn tiền " + (payment != null && payment.getRefundAmount() != null ? payment.getRefundAmount() : refundAmount) + " VND)"
                : cancelReason;

        getOrderSupport().recordHistory(order, previousStatus, OrderStatus.CANCELLED, getOrderSupport().normalizeActorType(actorType), actorIdentifier, historyReason);
        OrderEntity savedOrder = orderRepository.save(order);

        // 7. Gửi email xác nhận hủy đơn và hoàn tiền cho khách hàng
        if (wasPaid && payment != null) {
            try {
                emailService.sendRefundEmail(savedOrder, payment, refundAmount, cancelReason);
            } catch (Exception ex) {
                log.error("Failed to send refund email for order #{}: {}", savedOrder.getId(), ex.getMessage());
            }
        }

        return orderMapper.toResponse(savedOrder);
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus targetStatus, String actorType) {
        if (currentStatus == targetStatus) {
            throw new OrderProcessingException("Đơn hàng đã ở trạng thái: " + targetStatus);
        }

        switch (currentStatus) {
            case DRAFT -> {
                if (targetStatus == OrderStatus.PENDING_APPROVAL || targetStatus == OrderStatus.PENDING_PO_REVIEW) {
                    if (!isCustomerActor(actorType)) {
                        throw new OrderProcessingException("Chỉ khách hàng mới có thể nộp đơn nháp lên PO duyệt.");
                    }
                    return;
                }
                if (targetStatus == OrderStatus.CANCELLED) {
                    if (!isCustomerActor(actorType) && !isOperatorActor(actorType)) {
                        throw new OrderProcessingException("Chỉ khách hàng hoặc Admin mới có thể hủy đơn nháp.");
                    }
                    return;
                }
            }
            case PENDING_APPROVAL, PENDING_PO_REVIEW -> {
                if (targetStatus == OrderStatus.AWAITING_PAYMENT || targetStatus == OrderStatus.REJECTED || targetStatus == OrderStatus.RETURNED || targetStatus == OrderStatus.ACTION_REQUIRED) {
                    if (!isProductOwnerActor(actorType)) {
                        throw new OrderProcessingException("Chỉ Product Owner hoặc Admin mới có quyền duyệt, từ chối hoặc trả lại đơn hàng.");
                    }
                    return;
                }
                if (targetStatus == OrderStatus.CANCELLED) {
                    if (!isCustomerActor(actorType) && !isOperatorActor(actorType)) {
                        throw new OrderProcessingException("Chỉ khách hàng hoặc Admin mới có thể hủy đơn hàng đang chờ duyệt.");
                    }
                    return;
                }
            }
            case ACTION_REQUIRED -> {
                if (targetStatus == OrderStatus.AWAITING_PAYMENT || targetStatus == OrderStatus.CANCELLED) {
                    if (!isCustomerActor(actorType) && !isOperatorActor(actorType)) {
                        throw new OrderProcessingException("Chỉ khách hàng hoặc Admin mới có thể đưa ra quyết định xử lý đơn hàng.");
                    }
                    return;
                }
            }
            case RETURNED -> {
                if (targetStatus == OrderStatus.PENDING_APPROVAL || targetStatus == OrderStatus.PENDING_PO_REVIEW) {
                    if (!isCustomerActor(actorType)) {
                        throw new OrderProcessingException("Chỉ khách hàng mới có thể nộp lại đơn hàng đã chỉnh sửa.");
                    }
                    return;
                }
                if (targetStatus == OrderStatus.CANCELLED) {
                    if (!isCustomerActor(actorType) && !isOperatorActor(actorType)) {
                        throw new OrderProcessingException("Chỉ khách hàng hoặc Admin mới có thể hủy đơn hàng bị trả lại.");
                    }
                    return;
                }
            }
            case AWAITING_PAYMENT -> {
                if (targetStatus == OrderStatus.PAID) {
                    if (!isPaymentGatewayActor(actorType)) {
                        throw new OrderProcessingException("Chỉ cổng thanh toán hoặc hệ thống mới có thể xác nhận đơn hàng đã thanh toán.");
                    }
                    return;
                }
                if (targetStatus == OrderStatus.CANCELLED) {
                    if (!isCustomerActor(actorType) && !isOperatorActor(actorType) && !isPaymentGatewayActor(actorType)) {
                        throw new OrderProcessingException("Chỉ khách hàng, Admin hoặc Cổng thanh toán mới có thể hủy đơn hàng chưa thanh toán.");
                    }
                    return;
                }
            }
            case PAID -> {
                if (targetStatus == OrderStatus.COMPLETED) {
                    if (!isOperatorActor(actorType) && !isCustomerActor(actorType)) {
                        throw new OrderProcessingException("Chỉ khách hàng hoặc Admin mới có thể hoàn tất đơn hàng.");
                    }
                    return;
                }
                if (targetStatus == OrderStatus.CANCELLED) {
                    if (!isCustomerActor(actorType) && !isOperatorActor(actorType)) {
                        throw new OrderProcessingException("Chỉ khách hàng hoặc Admin mới có thể hủy đơn hàng.");
                    }
                    return;
                }
            }
            case REJECTED, CANCELLED, COMPLETED -> {
                throw new OrderProcessingException("Không thể chuyển đổi trạng thái từ trạng thái kết thúc: " + currentStatus);
            }
        }
        throw new OrderProcessingException("Chuyển đổi trạng thái từ " + currentStatus + " sang " + targetStatus + " không hợp lệ.");
    }

    private boolean isPaymentGatewayActor(String actorType) {
        String normalized = getOrderSupport().normalizeActorType(actorType);
        return "VNPAY".equals(normalized) || SYSTEM_ACTOR.equals(normalized);
    }

    private boolean isProductOwnerActor(String actorType) {
        String normalized = getOrderSupport().normalizeActorType(actorType);
        return "PRODUCT_OWNER".equals(normalized) || "PO".equals(normalized) || "ADMIN".equals(normalized) || SYSTEM_ACTOR.equals(normalized);
    }

    private boolean isCustomerActor(String actorType) {
        String normalized = getOrderSupport().normalizeActorType(actorType);
        return "CUSTOMER".equals(normalized) || SYSTEM_ACTOR.equals(normalized);
    }

    private boolean isOperatorActor(String actorType) {
        String normalized = getOrderSupport().normalizeActorType(actorType);
        return "ADMIN".equals(normalized) || SYSTEM_ACTOR.equals(normalized);
    }
}
