package com.example.ecommerce.order.service.impl;

import com.example.ecommerce.auth.entity.UserEntity;
import com.example.ecommerce.auth.enums.UserRole;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.category.entity.CategoryEntity;
import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.common.exception.OrderProcessingException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.customer.enums.CustomerStatus;
import com.example.ecommerce.customer.repository.CustomerRepository;
import com.example.ecommerce.invoice.enums.InvoiceStatus;
import com.example.ecommerce.invoice.repository.InvoiceRepository;
import com.example.ecommerce.invoice.service.InvoiceService;
import com.example.ecommerce.mail.service.EmailService;
import com.example.ecommerce.payment.vnpay.service.VnPayService;
import com.example.ecommerce.order.dto.request.CustomerDecisionRequest;
import com.example.ecommerce.order.dto.request.OrderItemRequest;
import com.example.ecommerce.order.dto.request.OrderRequest;
import com.example.ecommerce.order.dto.request.OrderUpdateRequest;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.order.dto.response.OrderStatusHistoryResponse;
import com.example.ecommerce.order.dto.response.SubOrderResponse;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.OrderItemEntity;
import com.example.ecommerce.order.entity.OrderStatusHistoryEntity;
import com.example.ecommerce.order.entity.SubOrderEntity;
import com.example.ecommerce.order.enums.CustomerDecisionAction;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.enums.SubOrderStatus;
import com.example.ecommerce.order.mapper.OrderMapper;
import com.example.ecommerce.order.mapper.SubOrderMapper;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.order.repository.OrderStatusHistoryRepository;
import com.example.ecommerce.order.repository.SubOrderRepository;
import com.example.ecommerce.order.service.OrderItemService;
import com.example.ecommerce.order.service.OrderService;
import com.example.ecommerce.order.specification.OrderSpecification;
import com.example.ecommerce.payment.entity.PaymentEntity;
import com.example.ecommerce.payment.enums.PaymentMethod;
import com.example.ecommerce.payment.enums.PaymentStatus;
import com.example.ecommerce.payment.repository.PaymentRepository;
import com.example.ecommerce.product.entity.ProductEntity;
import com.example.ecommerce.ticket.entity.PurchaseTicketEntity;
import com.example.ecommerce.ticket.enums.TicketStatus;
import com.example.ecommerce.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;
    private final CustomerRepository customerRepository;
    private final OrderItemService orderItemService;
    private final OrderMapper orderMapper;
    private final SubOrderMapper subOrderMapper;
    private final PaymentRepository paymentRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;
    private final org.springframework.beans.factory.ObjectProvider<VnPayService> vnPayServiceProvider;
    private final EmailService emailService;

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public SubOrderResponse getSubOrderById(Long subOrderId) {
        SubOrderEntity subOrder = subOrderRepository.findById(subOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-order with id " + subOrderId + " not found."));
        return subOrderMapper.toResponse(subOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrders(Map<String, Object> params) {
        return findOrders(params, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrders(Map<String, Object> params, List<String> sortByParams) {
        return findOrders(params, sortByParams, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrdersByCustomerId(Long customerId, Map<String, Object> params, List<String> sortByParams) {
        CustomerEntity customer = customerRepository.findByIdAndDeletedFalse(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + customerId + " not found."));
        return findOrders(params, sortByParams, customer.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(OrderRequest request) {
        return createOrder(request, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(OrderRequest request, String username) {
        CustomerEntity customer = resolveCustomerForOrder(request, username);
        LocalDateTime now = LocalDateTime.now();

        // 1. Merge & validate requested products
        Map<Long, Integer> requestedQuantities = orderItemService.mergeRequestedQuantities(request.getOrderItems());
        Map<Long, ProductEntity> productById = orderItemService.validateAndLoadProductsForUpdate(requestedQuantities);

        // 2. Group items by PO (Category managedBy User)
        Map<UserEntity, List<OrderItemEntity>> itemsByPo = new HashMap<>();
        BigDecimal masterTotal = BigDecimal.ZERO;

        OrderEntity masterOrder = OrderEntity.builder()
                .customer(customer)
                .status(OrderStatus.PENDING_PO_REVIEW)
                .statusChangedAt(now)
                .totalAmount(BigDecimal.ZERO)
                .orderItems(new ArrayList<>())
                .subOrders(new ArrayList<>())
                .build();

        OrderEntity savedMasterOrder = orderRepository.save(masterOrder);
        savedMasterOrder.setOrderCode(generateMasterOrderCode(savedMasterOrder.getId()));

        int subOrderIndex = 1;
        for (Long productId : requestedQuantities.keySet()) {
            ProductEntity product = productById.get(productId);
            Integer quantity = requestedQuantities.get(productId);
            BigDecimal unitPrice = product.getPrice();
            BigDecimal subTotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
            masterTotal = masterTotal.add(subTotal);

            UserEntity poUser = (product.getCategory() != null) ? product.getCategory().getManagedBy() : null;

            OrderItemEntity item = OrderItemEntity.builder()
                    .order(savedMasterOrder)
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .subTotal(subTotal)
                    .build();

            itemsByPo.computeIfAbsent(poUser, k -> new ArrayList<>()).add(item);
            savedMasterOrder.getOrderItems().add(item);
        }

        savedMasterOrder.setTotalAmount(masterTotal);

        // 3. Create Sub Orders & Purchase Tickets for each PO group
        for (Map.Entry<UserEntity, List<OrderItemEntity>> entry : itemsByPo.entrySet()) {
            UserEntity poUser = entry.getKey();
            List<OrderItemEntity> subItems = entry.getValue();

            BigDecimal subOrderTotal = BigDecimal.ZERO;
            for (OrderItemEntity item : subItems) {
                subOrderTotal = subOrderTotal.add(item.getSubTotal());
            }

            String subOrderCode = generateSubOrderCode(savedMasterOrder.getId(), subOrderIndex++);

            SubOrderEntity subOrder = SubOrderEntity.builder()
                    .masterOrder(savedMasterOrder)
                    .managedBy(poUser)
                    .subOrderCode(subOrderCode)
                    .status(SubOrderStatus.PENDING_APPROVAL)
                    .statusChangedAt(now)
                    .totalAmount(subOrderTotal)
                    .orderItems(new ArrayList<>())
                    .build();

            SubOrderEntity savedSubOrder = subOrderRepository.save(subOrder);

            for (OrderItemEntity item : subItems) {
                item.setSubOrder(savedSubOrder);
                savedSubOrder.getOrderItems().add(item);
            }

            // Create purchase ticket for this SubOrder
            PurchaseTicketEntity ticket = PurchaseTicketEntity.builder()
                    .ticketCode(generateTicketCodeForSubOrder(savedSubOrder.getId()))
                    .order(savedMasterOrder)
                    .subOrder(savedSubOrder)
                    .customer(customer)
                    .reviewer(poUser)
                    .status(TicketStatus.PENDING_APPROVAL)
                    .title("Phiếu duyệt đơn con #" + subOrderCode + " của đơn hàng #" + savedMasterOrder.getId())
                    .description("Phiếu mua hàng đang chờ Product Owner (" + (poUser != null ? poUser.getUsername() : "Chung") + ") phê duyệt.")
                    .deleted(false)
                    .build();

            ticketRepository.save(ticket);
            savedSubOrder.setTicket(ticket);
            savedMasterOrder.getSubOrders().add(savedSubOrder);
        }

        // 4. Reserve stock for all ordered products
        orderItemService.decrementStock(productById, requestedQuantities);

        OrderEntity finalSavedOrder = orderRepository.save(savedMasterOrder);

        recordHistory(finalSavedOrder, null, OrderStatus.PENDING_PO_REVIEW, "CUSTOMER",
                username != null ? username : String.valueOf(customer.getId()),
                "Đơn hàng được tạo thành công với " + savedMasterOrder.getSubOrders().size() + " đơn con chờ các PO phê duyệt.");

        return orderMapper.toResponse(finalSavedOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createDraftOrder(OrderRequest request) {
        return createDraftOrder(request, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createDraftOrder(OrderRequest request, String username) {
        CustomerEntity customer = resolveCustomerForOrder(request, username);
        LocalDateTime now = LocalDateTime.now();

        Map<Long, Integer> requestedQuantities = orderItemService.mergeRequestedQuantities(request.getOrderItems());
        Map<Long, ProductEntity> productById = orderItemService.validateAndLoadProducts(requestedQuantities);

        OrderEntity order = new OrderEntity();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);
        order.setStatusChangedAt(now);

        OrderEntity savedOrder = orderRepository.save(order);
        savedOrder.setOrderCode(generateMasterOrderCode(savedOrder.getId()));

        List<OrderItemEntity> orderItems = orderItemService.createOrderItems(savedOrder, productById, requestedQuantities);
        savedOrder.setOrderItems(orderItems);
        savedOrder.setTotalAmount(orderItemService.calculateTotalAmount(orderItems));

        OrderEntity finalSavedOrder = orderRepository.save(savedOrder);
        recordHistory(finalSavedOrder, null, OrderStatus.DRAFT, "CUSTOMER", username != null ? username : String.valueOf(customer.getId()), "Created draft order");
        return orderMapper.toResponse(finalSavedOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse submitOrder(Long orderId, String actorType, String actorIdentifier) {
        OrderEntity order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));

        if (order.getStatus() != OrderStatus.DRAFT && order.getStatus() != OrderStatus.RETURNED) {
            throw new OrderProcessingException("Only DRAFT or RETURNED orders can be submitted for approval. Current status: " + order.getStatus());
        }

        // Submits draft order by creating sub-orders and reserving stock
        List<OrderItemRequest> itemRequests = order.getOrderItems().stream()
                .map(item -> {
                    OrderItemRequest req = new OrderItemRequest();
                    req.setProductId(item.getProduct().getId());
                    req.setQuantity(item.getQuantity());
                    return req;
                })
                .toList();

        Map<Long, Integer> requestedQuantities = orderItemService.mergeRequestedQuantities(itemRequests);
        Map<Long, ProductEntity> productById = orderItemService.validateAndLoadProductsForUpdate(requestedQuantities);
        orderItemService.decrementStock(productById, requestedQuantities);

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(OrderStatus.PENDING_PO_REVIEW);
        order.setStatusChangedAt(now);

        // Group items and create sub-orders if not already present
        if (order.getSubOrders() == null || order.getSubOrders().isEmpty()) {
            Map<UserEntity, List<OrderItemEntity>> itemsByPo = new HashMap<>();
            for (OrderItemEntity item : order.getOrderItems()) {
                UserEntity poUser = (item.getProduct().getCategory() != null) ? item.getProduct().getCategory().getManagedBy() : null;
                itemsByPo.computeIfAbsent(poUser, k -> new ArrayList<>()).add(item);
            }

            int subIndex = 1;
            for (Map.Entry<UserEntity, List<OrderItemEntity>> entry : itemsByPo.entrySet()) {
                UserEntity poUser = entry.getKey();
                List<OrderItemEntity> subItems = entry.getValue();

                BigDecimal subOrderTotal = BigDecimal.ZERO;
                for (OrderItemEntity item : subItems) {
                    subOrderTotal = subOrderTotal.add(item.getSubTotal());
                }

                String subOrderCode = generateSubOrderCode(order.getId(), subIndex++);
                SubOrderEntity subOrder = SubOrderEntity.builder()
                        .masterOrder(order)
                        .managedBy(poUser)
                        .subOrderCode(subOrderCode)
                        .status(SubOrderStatus.PENDING_APPROVAL)
                        .statusChangedAt(now)
                        .totalAmount(subOrderTotal)
                        .orderItems(new ArrayList<>())
                        .build();

                SubOrderEntity savedSubOrder = subOrderRepository.save(subOrder);
                for (OrderItemEntity item : subItems) {
                    item.setSubOrder(savedSubOrder);
                    savedSubOrder.getOrderItems().add(item);
                }

                PurchaseTicketEntity ticket = PurchaseTicketEntity.builder()
                        .ticketCode(generateTicketCodeForSubOrder(savedSubOrder.getId()))
                        .order(order)
                        .subOrder(savedSubOrder)
                        .customer(order.getCustomer())
                        .reviewer(poUser)
                        .status(TicketStatus.PENDING_APPROVAL)
                        .title("Phiếu duyệt đơn con #" + subOrderCode + " của đơn hàng #" + order.getId())
                        .deleted(false)
                        .build();

                ticketRepository.save(ticket);
                savedSubOrder.setTicket(ticket);
                order.getSubOrders().add(savedSubOrder);
            }
        }

        recordHistory(order, OrderStatus.DRAFT, OrderStatus.PENDING_PO_REVIEW, normalizeActorType(actorType), actorIdentifier, "Submitted order ticket for PO approval");
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse approveSubOrder(Long subOrderId, String poUsername, String notes) {
        SubOrderEntity subOrder = subOrderRepository.findByIdForUpdate(subOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-order with id " + subOrderId + " not found."));

        if (subOrder.getStatus() != SubOrderStatus.PENDING_APPROVAL) {
            throw new OrderProcessingException("Only PENDING_APPROVAL sub-orders can be approved. Current status: " + subOrder.getStatus());
        }

        UserEntity poUser = validateAndGetPoUser(poUsername);
        validatePoOwnershipForSubOrder(subOrder, poUser);

        LocalDateTime now = LocalDateTime.now();
        subOrder.setStatus(SubOrderStatus.APPROVED);
        subOrder.setStatusChangedAt(now);
        subOrder.setApprovedBy(poUser);
        subOrder.setApprovedAt(now);
        subOrder.setReviewNotes(trimToNull(notes));
        subOrder.setRejectedReason(null);
        subOrder.setReturnedReason(null);
        subOrderRepository.save(subOrder);

        // Update SubOrder's Ticket
        ticketRepository.findBySubOrderId(subOrder.getId()).ifPresent(ticket -> {
            ticket.setStatus(TicketStatus.APPROVED);
            ticket.setReviewer(poUser);
            ticket.setReviewedAt(now);
            ticket.setReviewNotes(trimToNull(notes));
            ticketRepository.save(ticket);
        });

        OrderEntity masterOrder = orderRepository.findByIdForUpdate(subOrder.getMasterOrder().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Master order not found"));

        evaluateAndSyncMasterOrderStatus(masterOrder, poUsername, "PO approved sub-order #" + subOrder.getSubOrderCode());
        return orderMapper.toResponse(orderRepository.save(masterOrder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse rejectSubOrder(Long subOrderId, String poUsername, String reason) {
        SubOrderEntity subOrder = subOrderRepository.findByIdForUpdate(subOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-order with id " + subOrderId + " not found."));

        if (subOrder.getStatus() != SubOrderStatus.PENDING_APPROVAL) {
            throw new OrderProcessingException("Only PENDING_APPROVAL sub-orders can be rejected. Current status: " + subOrder.getStatus());
        }

        String validReason = trimToNull(reason);
        if (validReason == null) {
            throw new BadRequestException("Lý do từ chối không được để trống.");
        }

        UserEntity poUser = validateAndGetPoUser(poUsername);
        validatePoOwnershipForSubOrder(subOrder, poUser);

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse returnSubOrder(Long subOrderId, String poUsername, String reason) {
        SubOrderEntity subOrder = subOrderRepository.findByIdForUpdate(subOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-order with id " + subOrderId + " not found."));

        if (subOrder.getStatus() != SubOrderStatus.PENDING_APPROVAL) {
            throw new OrderProcessingException("Only PENDING_APPROVAL sub-orders can be returned. Current status: " + subOrder.getStatus());
        }

        String validReason = trimToNull(reason);
        if (validReason == null) {
            throw new BadRequestException("Lý do trả lại không được để trống.");
        }

        UserEntity poUser = validateAndGetPoUser(poUsername);
        validatePoOwnershipForSubOrder(subOrder, poUser);

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse handleCustomerDecision(Long orderId, CustomerDecisionRequest request, String customerUsername) {
        OrderEntity masterOrder = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Master order with id " + orderId + " not found."));

        if (masterOrder.getStatus() != OrderStatus.ACTION_REQUIRED) {
            throw new OrderProcessingException("Chỉ đơn hàng ở trạng thái ACTION_REQUIRED mới có thể đưa ra quyết định. Trạng thái hiện tại: " + masterOrder.getStatus());
        }

        validateCustomerOwnershipForMasterOrder(masterOrder, customerUsername);

        LocalDateTime now = LocalDateTime.now();
        if (request.getAction() == CustomerDecisionAction.ACCEPT_PARTIAL) {
            // 1. Restore stock for rejected/returned sub-orders and mark them CANCELLED
            List<SubOrderEntity> subOrders = subOrderRepository.findByMasterOrderId(masterOrder.getId());
            for (SubOrderEntity subOrder : subOrders) {
                if (subOrder.getStatus() == SubOrderStatus.REJECTED || subOrder.getStatus() == SubOrderStatus.RETURNED) {
                    restoreStockForSubOrder(subOrder);
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

            // 3. Update master order to PAYMENT_PENDING
            masterOrder.setTotalAmount(newTotalAmount);
            masterOrder.setStatus(OrderStatus.PAYMENT_PENDING);
            masterOrder.setStatusChangedAt(now);
            masterOrder.setPaymentExpiresAt(now.plusHours(24));
            masterOrder.setCancelReason(null);

            // 4. Update or create Payment
            PaymentEntity payment = resolvePayment(masterOrder);
            if (payment == null) {
                payment = PaymentEntity.builder()
                        .totalAmount(newTotalAmount)
                        .order(masterOrder)
                        .paymentMethod(PaymentMethod.VNPAY)
                        .status(PaymentStatus.PENDING)
                        .transactionRef(generateTransactionRef(masterOrder.getId()))
                        .build();
            } else {
                payment.setStatus(PaymentStatus.PENDING);
                payment.setTotalAmount(newTotalAmount);
                payment.setTransactionRef(generateTransactionRef(masterOrder.getId()));
            }
            paymentRepository.save(payment);

            recordHistory(masterOrder, OrderStatus.ACTION_REQUIRED, OrderStatus.PAYMENT_PENDING, "CUSTOMER", customerUsername,
                    "Khách hàng đồng ý tiếp tục các đơn hàng con được duyệt (Tổng tiền mới: " + newTotalAmount + ") và loại bỏ các đơn bị từ chối.");

        } else if (request.getAction() == CustomerDecisionAction.CANCEL_ALL) {
            String cancelNote = "Khách hàng hủy toàn bộ đơn hàng do có sản phẩm bị từ chối" + (request.getNotes() != null ? " (" + request.getNotes() + ")" : "");

            // 1. Restore stock for ALL active sub-orders
            List<SubOrderEntity> subOrders = subOrderRepository.findByMasterOrderId(masterOrder.getId());
            for (SubOrderEntity subOrder : subOrders) {
                if (subOrder.getStatus() != SubOrderStatus.CANCELLED) {
                    restoreStockForSubOrder(subOrder);
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

            updatePendingPaymentStatus(masterOrder, PaymentStatus.CANCELLED);

            recordHistory(masterOrder, OrderStatus.ACTION_REQUIRED, OrderStatus.CANCELLED, "CUSTOMER", customerUsername, cancelNote);
        }

        return orderMapper.toResponse(orderRepository.save(masterOrder));
    }

    /**
     * Đồng bộ trạng thái Master Order dựa trên trạng thái của tất cả Sub Orders con.
     */
    private void evaluateAndSyncMasterOrderStatus(OrderEntity masterOrder, String poUsername, String reason) {
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
            // Vẫn còn ít nhất 1 PO chưa xử lý xong
            masterOrder.setStatus(OrderStatus.PENDING_PO_REVIEW);
            masterOrder.setStatusChangedAt(now);
            recordHistory(masterOrder, currentStatus, OrderStatus.PENDING_PO_REVIEW, "PRODUCT_OWNER", poUsername, reason + " (Đang chờ các PO còn lại)");
            return;
        }

        // Tất cả PO đã xử lý xong:
        if (approvedCount == total) {
            // Tất cả đơn con đều được APPROVED -> Chuyển sang PAYMENT_PENDING
            masterOrder.setStatus(OrderStatus.PAYMENT_PENDING);
            masterOrder.setStatusChangedAt(now);
            masterOrder.setPaymentExpiresAt(now.plusHours(24));

            PaymentEntity payment = resolvePayment(masterOrder);
            if (payment == null) {
                payment = PaymentEntity.builder()
                        .totalAmount(masterOrder.getTotalAmount())
                        .order(masterOrder)
                        .paymentMethod(PaymentMethod.VNPAY)
                        .status(PaymentStatus.PENDING)
                        .transactionRef(generateTransactionRef(masterOrder.getId()))
                        .build();
            } else {
                payment.setStatus(PaymentStatus.PENDING);
                payment.setTotalAmount(masterOrder.getTotalAmount());
            }
            paymentRepository.save(payment);

            recordHistory(masterOrder, currentStatus, OrderStatus.PAYMENT_PENDING, "PRODUCT_OWNER", poUsername,
                    "Tất cả các PO đã phê duyệt toàn bộ đơn hàng. Đơn hàng chuyển sang chờ thanh toán.");

        } else if (rejectedCount == total) {
            // Tất cả đơn con đều bị REJECTED -> Chuyển sang REJECTED / CANCELLED và hoàn kho
            restoreStock(masterOrder);
            masterOrder.setStatus(OrderStatus.REJECTED);
            masterOrder.setStatusChangedAt(now);
            masterOrder.setRejectedReason("Tất cả các Product Owner đã từ chối duyệt các sản phẩm trong đơn.");
            updatePendingPaymentStatus(masterOrder, PaymentStatus.CANCELLED);

            recordHistory(masterOrder, currentStatus, OrderStatus.REJECTED, "PRODUCT_OWNER", poUsername,
                    "Tất cả các Product Owner đã từ chối đơn hàng. Đơn hàng bị từ chối toàn bộ.");

        } else if (approvedCount > 0 && (rejectedCount > 0 || returnedCount > 0)) {
            // Một số đơn con APPROVED, một số bị REJECTED -> Chuyển sang ACTION_REQUIRED cho khách quyết định
            masterOrder.setStatus(OrderStatus.ACTION_REQUIRED);
            masterOrder.setStatusChangedAt(now);

            recordHistory(masterOrder, currentStatus, OrderStatus.ACTION_REQUIRED, "PRODUCT_OWNER", poUsername,
                    "Đơn hàng có một số sản phẩm được duyệt và một số sản phẩm bị từ chối. Chờ khách hàng đưa ra quyết định.");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse approveOrder(Long orderId, String poUsername, String notes) {
        OrderEntity masterOrder = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));

        UserEntity poUser = validateAndGetPoUser(poUsername);

        List<SubOrderEntity> subOrders = subOrderRepository.findByMasterOrderId(masterOrder.getId());
        if (subOrders.isEmpty()) {
            // Fallback for legacy monolithic orders
            validatePoCategoryOwnershipForOrder(masterOrder, poUsername);
            masterOrder.setStatus(OrderStatus.PAYMENT_PENDING);
            masterOrder.setStatusChangedAt(LocalDateTime.now());
            masterOrder.setPaymentExpiresAt(LocalDateTime.now().plusHours(24));
            masterOrder.setApprovedBy(poUser);
            masterOrder.setApprovedAt(LocalDateTime.now());
            masterOrder.setReviewNotes(trimToNull(notes));
            return orderMapper.toResponse(orderRepository.save(masterOrder));
        }

        // If PO: approve the sub-order belonging to this PO
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse rejectOrder(Long orderId, String poUsername, String reason) {
        OrderEntity masterOrder = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));

        UserEntity poUser = validateAndGetPoUser(poUsername);

        List<SubOrderEntity> subOrders = subOrderRepository.findByMasterOrderId(masterOrder.getId());
        if (subOrders.isEmpty()) {
            validatePoCategoryOwnershipForOrder(masterOrder, poUsername);
            restoreStock(masterOrder);
            masterOrder.setStatus(OrderStatus.REJECTED);
            masterOrder.setStatusChangedAt(LocalDateTime.now());
            masterOrder.setRejectedReason(trimToNull(reason));
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse returnOrder(Long orderId, String poUsername, String reason) {
        OrderEntity masterOrder = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));

        UserEntity poUser = validateAndGetPoUser(poUsername);

        List<SubOrderEntity> subOrders = subOrderRepository.findByMasterOrderId(masterOrder.getId());
        if (subOrders.isEmpty()) {
            validatePoCategoryOwnershipForOrder(masterOrder, poUsername);
            restoreStock(masterOrder);
            masterOrder.setStatus(OrderStatus.RETURNED);
            masterOrder.setStatusChangedAt(LocalDateTime.now());
            masterOrder.setReturnedReason(trimToNull(reason));
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse updateReturnedOrder(Long orderId, OrderUpdateRequest request, String actorType, String actorIdentifier) {
        OrderEntity order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));

        if (order.getStatus() != OrderStatus.RETURNED && order.getStatus() != OrderStatus.ACTION_REQUIRED) {
            throw new OrderProcessingException("Chỉ có đơn hàng RETURNED hoặc ACTION_REQUIRED mới có thể cập nhật và gửi duyệt lại.");
        }

        // Recreate order items and reserve new stock
        Map<Long, Integer> requestedQuantities = orderItemService.mergeRequestedQuantities(request.getOrderItems());
        Map<Long, ProductEntity> productById = orderItemService.validateAndLoadProductsForUpdate(requestedQuantities);

        order.getOrderItems().clear();
        List<OrderItemEntity> newOrderItems = orderItemService.createOrderItems(order, productById, requestedQuantities);
        orderItemService.decrementStock(productById, requestedQuantities);

        order.getOrderItems().addAll(newOrderItems);
        order.setTotalAmount(orderItemService.calculateTotalAmount(newOrderItems));
        order.setStatus(OrderStatus.PENDING_PO_REVIEW);
        order.setStatusChangedAt(LocalDateTime.now());

        recordHistory(order, OrderStatus.RETURNED, OrderStatus.PENDING_PO_REVIEW, normalizeActorType(actorType), actorIdentifier, "Updated items and resubmitted ticket for approval");
        return orderMapper.toResponse(orderRepository.save(order));
    }

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
            validatePoCategoryOwnershipForOrder(order, actorIdentifier);
        }

        validateStatusTransition(order.getStatus(), status, actorType);
        OrderStatus previousStatus = order.getStatus();
        order.setStatus(status);
        order.setStatusChangedAt(LocalDateTime.now());
        if (status != OrderStatus.PAYMENT_PENDING) {
            order.setCancelReason(null);
        }
        recordHistory(order, previousStatus, status, normalizeActorType(actorType), actorIdentifier, reason);
        OrderEntity savedOrder = orderRepository.save(order);

        // Sync Sub-orders status if advancing through lifecycle
        if (status == OrderStatus.PAID || status == OrderStatus.PROCESSING || status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED || status == OrderStatus.COMPLETED) {
            syncSubOrdersLifecycleStatus(savedOrder, status);
        }

        if (status == OrderStatus.PAID) {
            try {
                invoiceService.createInvoiceForOrder(savedOrder);
            } catch (Exception ex) {
                log.error("Failed to automatically create invoice for paid order #{}", savedOrder.getId(), ex);
            }
        }
        return orderMapper.toResponse(savedOrder);
    }

    private void syncSubOrdersLifecycleStatus(OrderEntity masterOrder, OrderStatus masterStatus) {
        List<SubOrderEntity> subOrders = subOrderRepository.findByMasterOrderId(masterOrder.getId());
        for (SubOrderEntity subOrder : subOrders) {
            if (subOrder.getStatus() == SubOrderStatus.APPROVED || subOrder.getStatus() == SubOrderStatus.PROCESSING || subOrder.getStatus() == SubOrderStatus.SHIPPED) {
                switch (masterStatus) {
                    case PAID, PROCESSING -> subOrder.setStatus(SubOrderStatus.PROCESSING);
                    case SHIPPED -> subOrder.setStatus(SubOrderStatus.SHIPPED);
                    case DELIVERED -> subOrder.setStatus(SubOrderStatus.DELIVERED);
                    case COMPLETED -> subOrder.setStatus(SubOrderStatus.COMPLETED);
                    default -> {}
                }
                subOrder.setStatusChangedAt(LocalDateTime.now());
                subOrderRepository.save(subOrder);
            }
        }
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId, String actorType, String actorIdentifier, String reason) {
        OrderEntity order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));
        return cancelOrderInternal(order, actorType, actorIdentifier, reason);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getOrderStatusHistory(Long orderId) {
        return orderStatusHistoryRepository.findAllByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(history -> OrderStatusHistoryResponse.builder()
                        .id(history.getId())
                        .fromStatus(history.getFromStatus())
                        .toStatus(history.getToStatus())
                        .actorType(history.getActorType())
                        .actorIdentifier(history.getActorIdentifier())
                        .reason(history.getReason())
                        .createdAt(history.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void cancelExpiredOrders() {
        LocalDateTime cutOffTime = LocalDateTime.now();
        List<OrderEntity> expiredOrders = orderRepository.findByStatusAndPaymentExpiresAtBefore(OrderStatus.PAYMENT_PENDING, cutOffTime);
        for (OrderEntity order : expiredOrders) {
            cancelExpiredOrder(order);
        }
    }

    private PageResponse<OrderResponse> findOrders(Map<String, Object> params, List<String> sortByParams, Long customerId) {
        OrderFilterCriteria criteria = resolveFilterCriteria(params, customerId);
        Pageable pageable = createPageable(params, sortByParams);
        Specification<OrderEntity> spec = OrderSpecification.withFilters(
                criteria.customerId(),
                criteria.status(),
                toStartOfDay(criteria.createdFrom()),
                toEndOfDay(criteria.createdTo()),
                criteria.minTotalAmount(),
                criteria.maxTotalAmount(),
                criteria.keyword()
        );
        Page<OrderEntity> page = orderRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(orderMapper::toResponse));
    }

    private CustomerEntity resolveCustomerForOrder(OrderRequest request, String username) {
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

    private void validatePoOwnershipForSubOrder(SubOrderEntity subOrder, UserEntity poUser) {
        if (poUser.getRole() == UserRole.ADMIN) {
            return;
        }
        if (subOrder.getManagedBy() != null && !subOrder.getManagedBy().getId().equals(poUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xử lý đơn hàng con #" + subOrder.getSubOrderCode() + " vì do PO '" + subOrder.getManagedBy().getUsername() + "' quản lý.");
        }
    }

    private void validateCustomerOwnershipForMasterOrder(OrderEntity masterOrder, String customerUsername) {
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

    private UserEntity validateAndGetPoUser(String poUsername) {
        if (poUsername == null || poUsername.isBlank()) {
            throw new BadRequestException("Username người phê duyệt không được để trống.");
        }
        return userRepository.findByUsername(poUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User " + poUsername + " not found."));
    }

    private void restoreStockForSubOrder(SubOrderEntity subOrder) {
        if (subOrder.getOrderItems() == null) return;
        for (OrderItemEntity item : subOrder.getOrderItems()) {
            ProductEntity product = item.getProduct();
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
            }
        }
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
                if (targetStatus == OrderStatus.PAYMENT_PENDING || targetStatus == OrderStatus.REJECTED || targetStatus == OrderStatus.RETURNED || targetStatus == OrderStatus.ACTION_REQUIRED) {
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
                if (targetStatus == OrderStatus.PAYMENT_PENDING || targetStatus == OrderStatus.CANCELLED) {
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
            case PAYMENT_PENDING -> {
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
                if (targetStatus == OrderStatus.PROCESSING) {
                    if (!isOperatorActor(actorType)) {
                        throw new OrderProcessingException("Chỉ Admin hoặc Hệ thống mới có thể chuyển đơn hàng sang trạng thái đang xử lý.");
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
            case PROCESSING -> {
                if (targetStatus == OrderStatus.SHIPPED) {
                    if (!isOperatorActor(actorType)) {
                        throw new OrderProcessingException("Chỉ Admin hoặc Hệ thống mới có thể chuyển đơn hàng sang trạng thái đang giao hàng.");
                    }
                    return;
                }
                if (targetStatus == OrderStatus.CANCELLED) {
                    if (!isCustomerActor(actorType) && !isOperatorActor(actorType)) {
                        throw new OrderProcessingException("Chỉ khách hàng hoặc Admin mới có thể hủy đơn hàng đang xử lý.");
                    }
                    return;
                }
            }
            case SHIPPED -> {
                if (targetStatus == OrderStatus.DELIVERED) {
                    if (!isOperatorActor(actorType)) {
                        throw new OrderProcessingException("Chỉ Admin hoặc Hệ thống mới có thể xác nhận đơn hàng đã giao thành công.");
                    }
                    return;
                }
            }
            case DELIVERED -> {
                if (targetStatus == OrderStatus.COMPLETED) {
                    if (!isOperatorActor(actorType) && !isCustomerActor(actorType)) {
                        throw new OrderProcessingException("Chỉ khách hàng hoặc Admin mới có thể hoàn tất đơn hàng.");
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

    private OrderResponse cancelOrderInternal(OrderEntity order, String actorType, String actorIdentifier, String reason) {
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderProcessingException("Không thể hủy đơn hàng ở trạng thái: " + order.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        String cancelReason = reason != null && !reason.isBlank() ? reason.trim() : "Khách hàng yêu cầu hủy đơn hàng";
        boolean wasPaid = (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.PROCESSING);

        // 1. Hoàn trả số lượng hàng tồn kho nếu không phải đơn nháp
        if (order.getStatus() != OrderStatus.DRAFT) {
            restoreStock(order);
        }

        PaymentEntity payment = resolvePayment(order);
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

        recordHistory(order, previousStatus, OrderStatus.CANCELLED, normalizeActorType(actorType), actorIdentifier, historyReason);
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

    private void cancelExpiredOrder(OrderEntity order) {
        LocalDateTime now = LocalDateTime.now();
        restoreStock(order);
        updatePendingPaymentStatus(order, PaymentStatus.EXPIRED);

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

        recordHistory(order, previousStatus, OrderStatus.CANCELLED, SYSTEM_ACTOR, "scheduler", "Expired payment window");
        orderRepository.save(order);
    }

    private void restoreStock(OrderEntity order) {
        if (order.getOrderItems() == null) return;
        for (OrderItemEntity orderItem : order.getOrderItems()) {
            ProductEntity product = orderItem.getProduct();
            if (product != null) {
                product.setStock(product.getStock() + orderItem.getQuantity());
            }
        }
    }

    private void updatePendingPaymentStatus(OrderEntity order, PaymentStatus paymentStatus) {
        PaymentEntity payment = resolvePayment(order);
        if (payment != null && payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(paymentStatus);
            paymentRepository.save(payment);
        }
    }

    private PaymentEntity resolvePayment(OrderEntity order) {
        if (order.getPayments() != null && !order.getPayments().isEmpty()) {
            return order.getPayments().get(0);
        }
        return paymentRepository.findByOrderId(order.getId()).orElse(null);
    }

    private void recordHistory(OrderEntity order, OrderStatus fromStatus, OrderStatus toStatus, String actorType, String actorIdentifier, String reason) {
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

    private String normalizeActorType(String actorType) {
        if (actorType == null || actorType.isBlank()) {
            return SYSTEM_ACTOR;
        }
        return actorType.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isPaymentGatewayActor(String actorType) {
        String normalized = normalizeActorType(actorType);
        return "VNPAY".equals(normalized) || SYSTEM_ACTOR.equals(normalized);
    }

    private boolean isProductOwnerActor(String actorType) {
        String normalized = normalizeActorType(actorType);
        return "PRODUCT_OWNER".equals(normalized) || "PO".equals(normalized) || "ADMIN".equals(normalized) || SYSTEM_ACTOR.equals(normalized);
    }

    private boolean isCustomerActor(String actorType) {
        String normalized = normalizeActorType(actorType);
        return "CUSTOMER".equals(normalized) || SYSTEM_ACTOR.equals(normalized);
    }

    private boolean isOperatorActor(String actorType) {
        String normalized = normalizeActorType(actorType);
        return "ADMIN".equals(normalized) || SYSTEM_ACTOR.equals(normalized);
    }

    private String generateTransactionRef(Long orderId) {
        return "ORD" + orderId + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String generateMasterOrderCode(Long orderId) {
        return "MO-" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now()) + "-" + String.format("%05d", orderId);
    }

    private String generateSubOrderCode(Long masterOrderId, int subIndex) {
        return "SO-" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now()) + "-" + String.format("%05d", masterOrderId) + "-P" + subIndex;
    }

    private String generateTicketCodeForSubOrder(Long subOrderId) {
        return "TK-SUB-" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now()) + "-" + String.format("%05d", subOrderId);
    }

    private void validatePoCategoryOwnershipForOrder(OrderEntity order, String poUsername) {
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

    private Pageable createPageable(Map<String, Object> params, List<String> sortByParams) {
        int page = DEFAULT_PAGE;
        int size = DEFAULT_SIZE;

        if (params != null) {
            if (params.containsKey("page") && params.get("page") != null) {
                try {
                    int parsedPage = Integer.parseInt(params.get("page").toString().trim());
                    if (parsedPage < 0) {
                        throw new BadRequestException("Số trang phải lớn hơn hoặc bằng 0.");
                    }
                    page = parsedPage;
                } catch (NumberFormatException ex) {
                    throw new BadRequestException("Tham số page phải là số nguyên hợp lệ.");
                }
            }
            if (params.containsKey("size") && params.get("size") != null) {
                try {
                    int parsedSize = Integer.parseInt(params.get("size").toString().trim());
                    if (parsedSize <= 0) {
                        throw new BadRequestException("Số lượng bản ghi phải lớn hơn 0.");
                    }
                    size = parsedSize;
                } catch (NumberFormatException ex) {
                    throw new BadRequestException("Tham số size phải là số nguyên hợp lệ.");
                }
            }
        }

        Sort.Direction defaultDirection = resolveDirection(params);
        List<Sort.Order> orders = buildSortOrders(params, sortByParams, defaultDirection);
        if (orders.isEmpty()) {
            orders.add(new Sort.Order(defaultDirection, "id"));
        }
        return PageRequest.of(page, size, Sort.by(orders));
    }

    private List<Sort.Order> buildSortOrders(Map<String, Object> params, List<String> sortByParams, Sort.Direction defaultDirection) {
        List<Sort.Order> orders = new ArrayList<>();
        List<String> rawTokens = new ArrayList<>();

        if (sortByParams != null && !sortByParams.isEmpty()) {
            for (String sortBy : sortByParams) {
                if (sortBy != null && !sortBy.isBlank()) {
                    rawTokens.add(sortBy.trim());
                }
            }
        }

        if (rawTokens.isEmpty() && params != null) {
            Object sortField = params.get("sortBy");
            if (sortField != null && !sortField.toString().isBlank()) {
                rawTokens.add(sortField.toString().trim());
            }
        }

        int i = 0;
        while (i < rawTokens.size()) {
            String token = rawTokens.get(i);
            if (token.isBlank()) {
                i++;
                continue;
            }

            if (token.contains(",") || token.contains(":")) {
                String delimiter = token.contains(":") ? ":" : ",";
                String[] parts = token.split(delimiter, 2);
                String field = parts[0].trim();
                if (!field.isBlank()) {
                    String mappedField = resolveSortBy(field);
                    Sort.Direction direction = defaultDirection;
                    if (parts.length == 2 && !parts[1].trim().isBlank()) {
                        direction = parseDirection(parts[1].trim(), "sortBy");
                    }
                    orders.add(new Sort.Order(direction, mappedField));
                }
                i++;
            } else {
                String mappedField = resolveSortBy(token);
                Sort.Direction direction = defaultDirection;
                if (i + 1 < rawTokens.size()) {
                    String nextToken = rawTokens.get(i + 1).trim();
                    if ("asc".equalsIgnoreCase(nextToken) || "desc".equalsIgnoreCase(nextToken)) {
                        direction = parseDirection(nextToken, "sortBy");
                        i++;
                    }
                }
                orders.add(new Sort.Order(direction, mappedField));
                i++;
            }
        }
        return orders;
    }

    private Sort.Direction resolveDirection(Map<String, Object> params) {
        Sort.Direction direction = Sort.Direction.DESC;
        if (params != null) {
            String dirStr = null;
            if (params.containsKey("direction") && params.get("direction") != null) {
                dirStr = params.get("direction").toString().trim();
            }
            if (dirStr != null && !dirStr.isBlank()) {
                direction = parseDirection(dirStr, "direction");
            }
        }
        return direction;
    }

    private Sort.Direction parseDirection(String direction, String fieldName) {
        if ("asc".equalsIgnoreCase(direction)) {
            return Sort.Direction.ASC;
        }
        if ("desc".equalsIgnoreCase(direction)) {
            return Sort.Direction.DESC;
        }
        if ("sortBy".equals(fieldName)) {
            throw new BadRequestException("sortBy phải là 'id', 'createdAt', 'status' hoặc 'totalAmount'.");
        }
        throw new BadRequestException("Direction phải là 'asc' hoặc 'desc'.");
    }

    private String resolveSortBy(String sortBy) {
        if (sortBy == null) {
            throw new BadRequestException("sortBy phải là 'id', 'createdAt', 'status' hoặc 'totalAmount'.");
        }
        return switch (sortBy.trim().toLowerCase()) {
            case "id" -> "id";
            case "createdat", "created_at" -> "createdAt";
            case "status" -> "status";
            case "totalamount", "total_amount" -> "totalAmount";
            default -> throw new BadRequestException("sortBy phải là 'id', 'createdAt', 'status' hoặc 'totalAmount'.");
        };
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime toEndOfDay(LocalDate date) {
        return date == null ? null : date.atTime(23, 59, 59, 999_999_999);
    }

    private OrderFilterCriteria resolveFilterCriteria(Map<String, Object> params, Long forcedCustomerId) {
        Long customerId = forcedCustomerId != null ? forcedCustomerId : parseLong(params, "customerId");
        OrderStatus status = parseOrderStatus(params, "status");
        LocalDate createdFrom = parseLocalDate(params, "createdFrom");
        LocalDate createdTo = parseLocalDate(params, "createdTo");
        BigDecimal minTotalAmount = parseBigDecimal(params, "minTotalAmount");
        BigDecimal maxTotalAmount = parseBigDecimal(params, "maxTotalAmount");
        String keyword = trimToNull(readParam(params, "keyword"));

        validateFilterRange(createdFrom, createdTo, minTotalAmount, maxTotalAmount);
        return new OrderFilterCriteria(customerId, status, createdFrom, createdTo, minTotalAmount, maxTotalAmount, keyword);
    }

    private void validateFilterRange(LocalDate createdFrom, LocalDate createdTo, BigDecimal minTotalAmount, BigDecimal maxTotalAmount) {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new BadRequestException("createdFrom phải nhỏ hơn hoặc bằng createdTo.");
        }
        if (minTotalAmount != null && maxTotalAmount != null && minTotalAmount.compareTo(maxTotalAmount) > 0) {
            throw new BadRequestException("minTotalAmount phải nhỏ hơn hoặc bằng maxTotalAmount.");
        }
    }

    private OrderStatus parseOrderStatus(Map<String, Object> params, String key) {
        String value = trimToNull(readParam(params, key));
        if (value == null) {
            return null;
        }
        try {
            return OrderStatus.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("status không hợp lệ.");
        }
    }

    private Long parseLong(Map<String, Object> params, String key) {
        String value = trimToNull(readParam(params, key));
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new BadRequestException(key + " phải là số nguyên hợp lệ.");
        }
    }

    private BigDecimal parseBigDecimal(Map<String, Object> params, String key) {
        String value = trimToNull(readParam(params, key));
        if (value == null) {
            return null;
        }
        try {
            BigDecimal amount = new BigDecimal(value);
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException(key + " phải lớn hơn hoặc bằng 0.");
            }
            return amount;
        } catch (NumberFormatException ex) {
            throw new BadRequestException(key + " phải là số hợp lệ.");
        }
    }

    private LocalDate parseLocalDate(Map<String, Object> params, String key) {
        String value = trimToNull(readParam(params, key));
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            throw new BadRequestException(key + " phải theo định dạng yyyy-MM-dd.");
        }
    }

    private String readParam(Map<String, Object> params, String key) {
        if (params == null) {
            return null;
        }
        Object value = params.get(key);
        return value == null ? null : value.toString();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record OrderFilterCriteria(
            Long customerId,
            OrderStatus status,
            LocalDate createdFrom,
            LocalDate createdTo,
            BigDecimal minTotalAmount,
            BigDecimal maxTotalAmount,
            String keyword
    ) {
    }
}
