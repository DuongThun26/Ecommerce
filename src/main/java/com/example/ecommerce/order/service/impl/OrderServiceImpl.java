package com.example.ecommerce.order.service.impl;

import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.common.exception.OrderProcessingException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.customer.repository.CustomerRepository;
import com.example.ecommerce.order.dto.request.OrderRequest;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.order.dto.response.OrderStatusHistoryResponse;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.OrderItemEntity;
import com.example.ecommerce.order.entity.OrderStatusHistoryEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.mapper.OrderMapper;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.order.repository.OrderStatusHistoryRepository;
import com.example.ecommerce.order.service.OrderItemService;
import com.example.ecommerce.order.service.OrderService;
import com.example.ecommerce.order.specification.OrderSpecification;
import com.example.ecommerce.payment.entity.PaymentEntity;
import com.example.ecommerce.payment.enums.PaymentMethod;
import com.example.ecommerce.payment.enums.PaymentStatus;
import com.example.ecommerce.payment.repository.PaymentRepository;
import com.example.ecommerce.product.entity.ProductEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.example.ecommerce.auth.entity.UserEntity;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.order.dto.request.OrderItemRequest;
import com.example.ecommerce.order.dto.request.OrderUpdateRequest;
import com.example.ecommerce.invoice.service.InvoiceService;
import com.example.ecommerce.ticket.entity.PurchaseTicketEntity;
import com.example.ecommerce.ticket.enums.TicketStatus;
import com.example.ecommerce.ticket.repository.TicketRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final OrderItemService orderItemService;
    private final OrderMapper orderMapper;
    private final PaymentRepository paymentRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final InvoiceService invoiceService;

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));
        return orderMapper.toResponse(order);
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
        OrderEntity order = new OrderEntity();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING_APPROVAL);
        order.setStatusChangedAt(now);
        order.setCancelReason(null);

        Map<Long, Integer> requestedQuantities = orderItemService.mergeRequestedQuantities(request.getOrderItems());
        Map<Long, ProductEntity> productById = orderItemService.validateAndLoadProductsForUpdate(requestedQuantities);
        List<OrderItemEntity> orderItems = orderItemService.createOrderItems(order, productById, requestedQuantities);
        orderItemService.decrementStock(productById, requestedQuantities);
        order.setOrderItems(orderItems);
        order.setTotalAmount(orderItemService.calculateTotalAmount(orderItems));

        OrderEntity savedOrder = orderRepository.save(order);

        // Create purchase ticket for PO review
        PurchaseTicketEntity ticket = PurchaseTicketEntity.builder()
                .ticketCode(generateTicketCode(savedOrder.getId()))
                .order(savedOrder)
                .customer(customer)
                .status(TicketStatus.PENDING_APPROVAL)
                .title("Phiếu mua hàng cho đơn hàng #" + savedOrder.getId())
                .description("Phiếu mua hàng đang chờ Product Owner phê duyệt.")
                .deleted(false)
                .build();
        ticketRepository.save(ticket);
        savedOrder.setTicket(ticket);

        recordHistory(savedOrder, null, OrderStatus.PENDING_APPROVAL, "CUSTOMER", username != null ? username : String.valueOf(customer.getId()), "Order ticket submitted for PO approval");
        return orderMapper.toResponse(savedOrder);
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
        OrderEntity order = new OrderEntity();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);
        order.setStatusChangedAt(now);
        order.setCancelReason(null);

        Map<Long, Integer> requestedQuantities = orderItemService.mergeRequestedQuantities(request.getOrderItems());
        Map<Long, ProductEntity> productById = orderItemService.validateAndLoadProducts(requestedQuantities);
        List<OrderItemEntity> orderItems = orderItemService.createOrderItems(order, productById, requestedQuantities);
        order.setOrderItems(orderItems);
        order.setTotalAmount(orderItemService.calculateTotalAmount(orderItems));

        OrderEntity savedOrder = orderRepository.save(order);
        recordHistory(savedOrder, null, OrderStatus.DRAFT, "CUSTOMER", username != null ? username : String.valueOf(customer.getId()), "Order draft created");
        return orderMapper.toResponse(savedOrder);
    }

    private CustomerEntity resolveCustomerForOrder(OrderRequest request, String username) {
        CustomerEntity customer = null;
        if (username != null && !username.isBlank()) {
            customer = customerRepository.findByUserUsername(username).orElse(null);
        }
        if (customer == null && request != null && request.getCustomerId() != null) {
            customer = customerRepository.findByIdAndDeletedFalse(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + request.getCustomerId() + " not found."));
        }
        if (customer == null && username != null && !username.isBlank()) {
            UserEntity user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                customer = CustomerEntity.builder()
                        .name(user.getUsername())
                        .email(user.getEmail() != null ? user.getEmail() : user.getUsername() + "@ecommerce.local")
                        .phone("0900000000")
                        .status(com.example.ecommerce.customer.enums.CustomerStatus.ACTIVE)
                        .deleted(false)
                        .user(user)
                        .build();
                customer = customerRepository.save(customer);
            }
        }
        if (customer == null) {
            throw new BadRequestException("Không tìm thấy thông tin khách hàng tương ứng với tài khoản đang đăng nhập.");
        }
        return customer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse submitOrder(Long orderId, String actorType, String actorIdentifier) {
        OrderEntity order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));

        if (order.getStatus() != OrderStatus.DRAFT && order.getStatus() != OrderStatus.RETURNED) {
            throw new OrderProcessingException("Only DRAFT or RETURNED orders can be submitted for approval. Current status: " + order.getStatus());
        }

        OrderStatus previousStatus = order.getStatus();
        if (previousStatus == OrderStatus.DRAFT) {
            // Reserve stock for draft items
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
        }

        order.setStatus(OrderStatus.PENDING_APPROVAL);
        order.setStatusChangedAt(LocalDateTime.now());

        // Create or reactivate purchase ticket
        PurchaseTicketEntity ticket = ticketRepository.findByOrderId(order.getId())
                .orElseGet(() -> PurchaseTicketEntity.builder()
                        .ticketCode(generateTicketCode(order.getId()))
                        .order(order)
                        .customer(order.getCustomer())
                        .title("Phiếu mua hàng cho đơn hàng #" + order.getId())
                        .description("Phiếu mua hàng đang chờ Product Owner phê duyệt.")
                        .deleted(false)
                        .build());
        ticket.setStatus(TicketStatus.PENDING_APPROVAL);
        ticket.setRejectionReason(null);
        ticket.setReturnedReason(null);
        ticketRepository.save(ticket);
        order.setTicket(ticket);

        recordHistory(order, previousStatus, OrderStatus.PENDING_APPROVAL, normalizeActorType(actorType), actorIdentifier, "Submitted order ticket for approval");
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse approveOrder(Long orderId, String poUsername, String notes) {
        OrderEntity order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));

        if (order.getStatus() != OrderStatus.PENDING_APPROVAL) {
            throw new OrderProcessingException("Only PENDING_APPROVAL orders can be approved by PO. Current status: " + order.getStatus());
        }

        UserEntity poUser = null;
        if (poUsername != null && !poUsername.isBlank()) {
            poUser = userRepository.findByUsername(poUsername).orElse(null);
        }

        LocalDateTime now = LocalDateTime.now();
        order.setApprovedBy(poUser);
        order.setApprovedAt(now);
        order.setReviewNotes(trimToNull(notes));
        order.setRejectedReason(null);
        order.setReturnedReason(null);
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        order.setStatusChangedAt(now);
        order.setPaymentExpiresAt(now.plusHours(24));

        OrderEntity savedOrder = orderRepository.save(order);

        // Update ticket
        ticketRepository.findByOrderId(savedOrder.getId()).ifPresent(ticket -> {
            ticket.setStatus(TicketStatus.APPROVED);
            ticket.setReviewer(order.getApprovedBy());
            ticket.setReviewedAt(now);
            ticket.setReviewNotes(order.getReviewNotes());
            ticketRepository.save(ticket);
            savedOrder.setTicket(ticket);
        });

        // Initialize or update payment entity for VNPay
        PaymentEntity payment = paymentRepository.findByOrderId(savedOrder.getId()).orElse(null);
        if (payment == null) {
            payment = PaymentEntity.builder()
                    .totalAmount(savedOrder.getTotalAmount())
                    .order(savedOrder)
                    .paymentMethod(PaymentMethod.VNPAY)
                    .status(PaymentStatus.PENDING)
                    .transactionRef(generateTransactionRef(savedOrder.getId()))
                    .build();
        } else {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setTotalAmount(savedOrder.getTotalAmount());
        }
        paymentRepository.save(payment);

        recordHistory(savedOrder, OrderStatus.PENDING_APPROVAL, OrderStatus.PAYMENT_PENDING, "PRODUCT_OWNER", poUsername, notes != null ? notes : "Approved by Product Owner");
        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse rejectOrder(Long orderId, String poUsername, String reason) {
        OrderEntity order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));

        if (order.getStatus() != OrderStatus.PENDING_APPROVAL) {
            throw new OrderProcessingException("Only PENDING_APPROVAL orders can be rejected by PO. Current status: " + order.getStatus());
        }

        String validReason = trimToNull(reason);
        if (validReason == null) {
            throw new BadRequestException("Lý do từ chối không được để trống.");
        }

        UserEntity poUser = null;
        if (poUsername != null && !poUsername.isBlank()) {
            poUser = userRepository.findByUsername(poUsername).orElse(null);
        }

        restoreStock(order);
        updatePendingPaymentStatus(order, PaymentStatus.CANCELLED);

        LocalDateTime now = LocalDateTime.now();
        order.setRejectedReason(validReason);
        order.setStatus(OrderStatus.REJECTED);
        order.setStatusChangedAt(now);

        // Update ticket
        UserEntity finalPoUser = poUser;
        ticketRepository.findByOrderId(order.getId()).ifPresent(ticket -> {
            ticket.setStatus(TicketStatus.REJECTED);
            ticket.setReviewer(finalPoUser);
            ticket.setReviewedAt(now);
            ticket.setRejectionReason(validReason);
            ticketRepository.save(ticket);
            order.setTicket(ticket);
        });

        recordHistory(order, OrderStatus.PENDING_APPROVAL, OrderStatus.REJECTED, "PRODUCT_OWNER", poUsername, validReason);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse returnOrder(Long orderId, String poUsername, String reason) {
        OrderEntity order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));

        if (order.getStatus() != OrderStatus.PENDING_APPROVAL) {
            throw new OrderProcessingException("Only PENDING_APPROVAL orders can be returned by PO. Current status: " + order.getStatus());
        }

        String validReason = trimToNull(reason);
        if (validReason == null) {
            throw new BadRequestException("Lý do trả lại không được để trống.");
        }

        UserEntity poUser = null;
        if (poUsername != null && !poUsername.isBlank()) {
            poUser = userRepository.findByUsername(poUsername).orElse(null);
        }

        // Release stock while ticket is returned for customer modification
        restoreStock(order);

        LocalDateTime now = LocalDateTime.now();
        order.setReturnedReason(validReason);
        order.setStatus(OrderStatus.RETURNED);
        order.setStatusChangedAt(now);

        // Update ticket
        UserEntity finalPoUser1 = poUser;
        ticketRepository.findByOrderId(order.getId()).ifPresent(ticket -> {
            ticket.setStatus(TicketStatus.RETURNED);
            ticket.setReviewer(finalPoUser1);
            ticket.setReviewedAt(now);
            ticket.setReturnedReason(validReason);
            ticketRepository.save(ticket);
            order.setTicket(ticket);
        });

        recordHistory(order, OrderStatus.PENDING_APPROVAL, OrderStatus.RETURNED, "PRODUCT_OWNER", poUsername, validReason);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse updateReturnedOrder(Long orderId, OrderUpdateRequest request, String actorType, String actorIdentifier) {
        OrderEntity order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));

        if (order.getStatus() != OrderStatus.RETURNED) {
            throw new OrderProcessingException("Only RETURNED orders can be updated and resubmitted. Current status: " + order.getStatus());
        }

        // Recreate order items and reserve new stock
        Map<Long, Integer> requestedQuantities = orderItemService.mergeRequestedQuantities(request.getOrderItems());
        Map<Long, ProductEntity> productById = orderItemService.validateAndLoadProductsForUpdate(requestedQuantities);

        order.getOrderItems().clear();
        List<OrderItemEntity> newOrderItems = orderItemService.createOrderItems(order, productById, requestedQuantities);
        orderItemService.decrementStock(productById, requestedQuantities);

        order.getOrderItems().addAll(newOrderItems);
        order.setTotalAmount(orderItemService.calculateTotalAmount(newOrderItems));
        order.setStatus(OrderStatus.PENDING_APPROVAL);
        order.setStatusChangedAt(LocalDateTime.now());

        // Reactivate ticket
        ticketRepository.findByOrderId(order.getId()).ifPresent(ticket -> {
            ticket.setStatus(TicketStatus.PENDING_APPROVAL);
            ticket.setRejectionReason(null);
            ticket.setReturnedReason(null);
            ticketRepository.save(ticket);
            order.setTicket(ticket);
        });

        recordHistory(order, OrderStatus.RETURNED, OrderStatus.PENDING_APPROVAL, normalizeActorType(actorType), actorIdentifier, "Updated items and resubmitted ticket for approval");
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

        validateStatusTransition(order.getStatus(), status, actorType);
        OrderStatus previousStatus = order.getStatus();
        order.setStatus(status);
        order.setStatusChangedAt(LocalDateTime.now());
        if (status != OrderStatus.PAYMENT_PENDING) {
            order.setCancelReason(null);
        }
        recordHistory(order, previousStatus, status, normalizeActorType(actorType), actorIdentifier, reason);
        OrderEntity savedOrder = orderRepository.save(order);
        if (status == OrderStatus.PAID) {
            try {
                invoiceService.createInvoiceForOrder(savedOrder);
            } catch (Exception ex) {
                log.error("Failed to automatically create invoice for paid order #{}", savedOrder.getId(), ex);
            }
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
        log.info("Cancelled {} expired orders.", expiredOrders.size());
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
            throw new BadRequestException("sortBy phải là 'id', 'createdAt', 'totalAmount' hoặc 'status'.");
        }
        throw new BadRequestException("Direction phải là 'asc' hoặc 'desc'.");
    }

    private String resolveSortBy(String sortBy) {
        if (sortBy == null) {
            throw new BadRequestException("sortBy phải là 'id', 'createdAt', 'totalAmount' hoặc 'status'.");
        }
        return switch (sortBy.trim().toLowerCase()) {
            case "id" -> "id";
            case "createdat", "created_at" -> "createdAt";
            case "totalamount", "total_amount" -> "totalAmount";
            case "status" -> "status";
            default -> throw new BadRequestException("sortBy phải là 'id', 'createdAt', 'totalAmount' hoặc 'status'.");
        };
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

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime toEndOfDay(LocalDate date) {
        return date == null ? null : date.atTime(23, 59, 59, 999_999_999);
    }

    private OrderFilterCriteria resolveFilterCriteria(Map<String, Object> params, Long customerId) {
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

    private BigDecimal parseBigDecimal(Map<String, Object> params, String key) {
        String value = trimToNull(readParam(params, key));
        if (value == null) {
            return null;
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new BadRequestException(key + " phải là số hợp lệ.");
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

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus nextStatus, String actorType) {
        if (currentStatus == nextStatus) {
            throw new OrderProcessingException("Order status is already " + currentStatus + ".");
        }

        boolean valid =
                (currentStatus == OrderStatus.DRAFT && nextStatus == OrderStatus.PENDING_APPROVAL && (isCustomerActor(actorType) || isOperatorActor(actorType)))
                        || (currentStatus == OrderStatus.DRAFT && nextStatus == OrderStatus.CANCELLED && (isCustomerActor(actorType) || isOperatorActor(actorType)))
                        || (currentStatus == OrderStatus.PENDING_APPROVAL && nextStatus == OrderStatus.PAYMENT_PENDING && (isProductOwnerActor(actorType) || isOperatorActor(actorType)))
                        || (currentStatus == OrderStatus.PENDING_APPROVAL && nextStatus == OrderStatus.RETURNED && (isProductOwnerActor(actorType) || isOperatorActor(actorType)))
                        || (currentStatus == OrderStatus.PENDING_APPROVAL && nextStatus == OrderStatus.REJECTED && (isProductOwnerActor(actorType) || isOperatorActor(actorType)))
                        || (currentStatus == OrderStatus.PENDING_APPROVAL && nextStatus == OrderStatus.CANCELLED && (isCustomerActor(actorType) || isOperatorActor(actorType)))
                        || (currentStatus == OrderStatus.RETURNED && nextStatus == OrderStatus.PENDING_APPROVAL && (isCustomerActor(actorType) || isOperatorActor(actorType)))
                        || (currentStatus == OrderStatus.RETURNED && nextStatus == OrderStatus.CANCELLED && (isCustomerActor(actorType) || isOperatorActor(actorType)))
                        || (currentStatus == OrderStatus.PAYMENT_PENDING && nextStatus == OrderStatus.PAID && (isPaymentGatewayActor(actorType) || isOperatorActor(actorType)))
                        || (currentStatus == OrderStatus.PAYMENT_PENDING && nextStatus == OrderStatus.CANCELLED && (isCustomerActor(actorType) || isOperatorActor(actorType)))
                        || (currentStatus == OrderStatus.PAID && nextStatus == OrderStatus.PROCESSING && isOperatorActor(actorType))
                        || (currentStatus == OrderStatus.PAID && nextStatus == OrderStatus.CANCELLED && isOperatorActor(actorType))
                        || (currentStatus == OrderStatus.PROCESSING && nextStatus == OrderStatus.SHIPPED && isOperatorActor(actorType))
                        || (currentStatus == OrderStatus.PROCESSING && nextStatus == OrderStatus.CANCELLED && isOperatorActor(actorType))
                        || (currentStatus == OrderStatus.SHIPPED && nextStatus == OrderStatus.DELIVERED && (isOperatorActor(actorType) || isCustomerActor(actorType)))
                        || (currentStatus == OrderStatus.DELIVERED && nextStatus == OrderStatus.COMPLETED && (isOperatorActor(actorType) || isCustomerActor(actorType)));

        if (!valid) {
            throw new OrderProcessingException("Invalid order status transition from " + currentStatus + " to " + nextStatus + " for actor " + actorType + ".");
        }
    }

    private OrderResponse cancelOrderInternal(OrderEntity order, String actorType, String actorIdentifier, String reason) {
        OrderStatus currentStatus = order.getStatus();
        if (currentStatus != OrderStatus.DRAFT
                && currentStatus != OrderStatus.PENDING_APPROVAL
                && currentStatus != OrderStatus.RETURNED
                && currentStatus != OrderStatus.PAYMENT_PENDING
                && currentStatus != OrderStatus.PAID) {
            throw new OrderProcessingException("Order in status " + currentStatus + " cannot be cancelled.");
        }

        if (currentStatus == OrderStatus.PENDING_APPROVAL || currentStatus == OrderStatus.PAYMENT_PENDING || currentStatus == OrderStatus.PAID) {
            restoreStock(order);
        }
        updatePendingPaymentStatus(order, PaymentStatus.CANCELLED);

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        order.setStatusChangedAt(LocalDateTime.now());

        // Cancel ticket if exists
        ticketRepository.findByOrderId(order.getId()).ifPresent(ticket -> {
            ticket.setStatus(TicketStatus.CANCELLED);
            ticketRepository.save(ticket);
            order.setTicket(ticket);
        });

        recordHistory(order, previousStatus, OrderStatus.CANCELLED, normalizeActorType(actorType), actorIdentifier, reason);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    private void cancelExpiredOrder(OrderEntity order) {
        restoreStock(order);
        updatePendingPaymentStatus(order, PaymentStatus.EXPIRED);

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason("Expired payment window");
        order.setStatusChangedAt(LocalDateTime.now());

        // Cancel ticket if exists
        ticketRepository.findByOrderId(order.getId()).ifPresent(ticket -> {
            ticket.setStatus(TicketStatus.CANCELLED);
            ticketRepository.save(ticket);
            order.setTicket(ticket);
        });

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

    private String generateTicketCode(Long orderId) {
        return "TK-" + java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now()) + "-" + String.format("%05d", orderId);
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
