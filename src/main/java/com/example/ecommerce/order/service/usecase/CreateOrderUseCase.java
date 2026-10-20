package com.example.ecommerce.order.service.usecase;

import com.example.ecommerce.auth.entity.UserEntity;
import com.example.ecommerce.common.exception.InsufficientStockException;
import com.example.ecommerce.common.exception.OrderProcessingException;
import com.example.ecommerce.common.exception.QuantityLimitExceededException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.order.dto.request.OrderItemRequest;
import com.example.ecommerce.order.dto.request.OrderRequest;
import com.example.ecommerce.order.dto.request.OrderUpdateRequest;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.OrderItemEntity;
import com.example.ecommerce.order.entity.SubOrderEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.enums.SubOrderStatus;
import com.example.ecommerce.order.mapper.OrderMapper;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.order.repository.SubOrderRepository;
import com.example.ecommerce.order.service.OrderItemService;
import com.example.ecommerce.order.service.support.OrderSupport;
import com.example.ecommerce.product.entity.ProductEntity;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;
    private final OrderItemService orderItemService;
    private final TicketRepository ticketRepository;
    private final OrderMapper orderMapper;
    private final OrderSupport orderSupport;
    private final SubOrderReviewUseCase subOrderReviewUseCase;

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(OrderRequest request, String username) {
        CustomerEntity customer = orderSupport.resolveCustomerForOrder(request, username);
        LocalDateTime now = LocalDateTime.now();

        // Check if customer already has an order in PENDING_APPROVAL (or PENDING_PO_REVIEW)
        List<OrderEntity> existingPendingOrders = orderRepository.findByCustomerIdAndStatusInForUpdate(
                customer.getId(),
                List.of(OrderStatus.PENDING_APPROVAL, OrderStatus.PENDING_PO_REVIEW)
        );

        if (existingPendingOrders != null && !existingPendingOrders.isEmpty()) {
            OrderEntity existingOrder = existingPendingOrders.get(0);
            return appendItemsToExistingPendingOrder(existingOrder, request, customer, username, now);
        }

        // 1. Merge & validate requested products
        Map<Long, Integer> requestedQuantities = orderItemService.mergeRequestedQuantities(request.getOrderItems());
        Map<Long, ProductEntity> productById = orderItemService.validateAndLoadProductsForUpdate(requestedQuantities);

        // 2. Group items by PO (Category managedBy User)
        Map<UserEntity, List<OrderItemEntity>> itemsByPo = new HashMap<>();
        BigDecimal masterTotal = BigDecimal.ZERO;

        OrderEntity masterOrder = OrderEntity.builder()
                .customer(customer)
                .status(OrderStatus.PENDING_APPROVAL)
                .statusChangedAt(now)
                .totalAmount(BigDecimal.ZERO)
                .orderItems(new ArrayList<>())
                .subOrders(new ArrayList<>())
                .build();

        OrderEntity savedMasterOrder = orderRepository.save(masterOrder);
        savedMasterOrder.setOrderCode(orderSupport.generateMasterOrderCode(savedMasterOrder.getId()));

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

            String subOrderCode = orderSupport.generateSubOrderCode(savedMasterOrder.getId(), subOrderIndex++);

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
                    .ticketCode(orderSupport.generateTicketCodeForSubOrder(savedSubOrder.getId()))
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

        orderSupport.recordHistory(finalSavedOrder, null, OrderStatus.PENDING_APPROVAL, "CUSTOMER",
                username != null ? username : String.valueOf(customer.getId()),
                "Đơn hàng được tạo thành công với " + savedMasterOrder.getSubOrders().size() + " đơn con chờ các PO phê duyệt.");

        return orderMapper.toResponse(finalSavedOrder);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createDraftOrder(OrderRequest request, String username) {
        CustomerEntity customer = orderSupport.resolveCustomerForOrder(request, username);
        LocalDateTime now = LocalDateTime.now();

        Map<Long, Integer> requestedQuantities = orderItemService.mergeRequestedQuantities(request.getOrderItems());
        Map<Long, ProductEntity> productById = orderItemService.validateAndLoadProducts(requestedQuantities);

        OrderEntity order = new OrderEntity();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);
        order.setStatusChangedAt(now);

        OrderEntity savedOrder = orderRepository.save(order);
        savedOrder.setOrderCode(orderSupport.generateMasterOrderCode(savedOrder.getId()));

        List<OrderItemEntity> orderItems = orderItemService.createOrderItems(savedOrder, productById, requestedQuantities);
        savedOrder.setOrderItems(orderItems);
        savedOrder.setTotalAmount(orderItemService.calculateTotalAmount(orderItems));

        OrderEntity finalSavedOrder = orderRepository.save(savedOrder);
        orderSupport.recordHistory(finalSavedOrder, null, OrderStatus.DRAFT, "CUSTOMER",
                username != null ? username : String.valueOf(customer.getId()), "Created draft order");
        return orderMapper.toResponse(finalSavedOrder);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse submitOrder(Long orderId, String actorType, String actorIdentifier) {
        OrderEntity order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found."));

        if (order.getStatus() != OrderStatus.DRAFT && order.getStatus() != OrderStatus.RETURNED) {
            throw new OrderProcessingException("Only DRAFT or RETURNED orders can be submitted for approval. Current status: " + order.getStatus());
        }

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

                String subOrderCode = orderSupport.generateSubOrderCode(order.getId(), subIndex++);
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
                        .ticketCode(orderSupport.generateTicketCodeForSubOrder(savedSubOrder.getId()))
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

        orderSupport.recordHistory(order, OrderStatus.DRAFT, OrderStatus.PENDING_PO_REVIEW,
                orderSupport.normalizeActorType(actorType), actorIdentifier, "Submitted order ticket for PO approval");
        return orderMapper.toResponse(orderRepository.save(order));
    }

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

        orderSupport.recordHistory(order, OrderStatus.RETURNED, OrderStatus.PENDING_PO_REVIEW,
                orderSupport.normalizeActorType(actorType), actorIdentifier, "Updated items and resubmitted ticket for approval");
        return orderMapper.toResponse(orderRepository.save(order));
    }

    private OrderResponse appendItemsToExistingPendingOrder(
            OrderEntity order,
            OrderRequest request,
            CustomerEntity customer,
            String username,
            LocalDateTime now
    ) {
        Map<Long, Integer> requestedQuantities = orderItemService.mergeRequestedQuantities(request.getOrderItems());
        Map<Long, ProductEntity> productById = orderItemService.validateAndLoadProductsForUpdate(requestedQuantities);

        for (Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer requestedQty = entry.getValue();
            ProductEntity product = productById.get(productId);

            if (product.getStock() < requestedQty) {
                throw new InsufficientStockException("Không đủ tồn kho cho sản phẩm " + product.getName() + ".");
            }

            if (requestedQty > 99) {
                throw new QuantityLimitExceededException("quantity của 1 item sau khi cộng KHÔNG ĐƯỢC vượt 99.");
            }
        }

        orderItemService.decrementStock(productById, requestedQuantities);

        if (order.getOrderItems() == null) {
            order.setOrderItems(new ArrayList<>());
        }
        if (order.getSubOrders() == null) {
            order.setSubOrders(new ArrayList<>());
        }

        for (Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer requestedQty = entry.getValue();
            ProductEntity product = productById.get(productId);
            UserEntity poUser = (product.getCategory() != null) ? product.getCategory().getManagedBy() : null;

            SubOrderEntity pendingSubOrder = orderSupport.findPendingSubOrderByPo(order, poUser);

            if (pendingSubOrder != null) {
                // Đã có suborder của PO này đang PENDING_APPROVAL -> cộng trực tiếp số lượng nếu đã có sản phẩm
                OrderItemEntity existingItem = orderSupport.findOrderItemInSubOrder(pendingSubOrder, productId);
                if (existingItem != null) {
                    int newQty = existingItem.getQuantity() + requestedQty;
                    if (newQty > 99) {
                        throw new QuantityLimitExceededException("quantity của 1 item sau khi cộng KHÔNG ĐƯỢC vượt 99.");
                    }
                    existingItem.setQuantity(newQty);
                    existingItem.setSubTotal(existingItem.getUnitPrice().multiply(BigDecimal.valueOf(newQty)).setScale(2, RoundingMode.HALF_UP));
                } else {
                    BigDecimal unitPrice = product.getPrice();
                    BigDecimal subTotal = unitPrice.multiply(BigDecimal.valueOf(requestedQty)).setScale(2, RoundingMode.HALF_UP);
                    OrderItemEntity newItem = OrderItemEntity.builder()
                            .order(order)
                            .subOrder(pendingSubOrder)
                            .product(product)
                            .quantity(requestedQty)
                            .unitPrice(unitPrice)
                            .subTotal(subTotal)
                            .build();

                    order.getOrderItems().add(newItem);
                    if (pendingSubOrder.getOrderItems() == null) {
                        pendingSubOrder.setOrderItems(new ArrayList<>());
                    }
                    pendingSubOrder.getOrderItems().add(newItem);
                }
            } else {
                // Không có suborder nào của PO này đang PENDING_APPROVAL (chưa có hoặc đã APPROVED hết)
                // -> Tạo 1 ordersub con khác và gửi PO duyệt riêng
                int nextIndex = (order.getSubOrders() != null ? order.getSubOrders().size() : 0) + 1;
                String subOrderCode = orderSupport.generateSubOrderCode(order.getId(), nextIndex);
                BigDecimal unitPrice = product.getPrice();
                BigDecimal subTotal = unitPrice.multiply(BigDecimal.valueOf(requestedQty)).setScale(2, RoundingMode.HALF_UP);

                SubOrderEntity newSubOrder = SubOrderEntity.builder()
                        .masterOrder(order)
                        .managedBy(poUser)
                        .subOrderCode(subOrderCode)
                        .status(SubOrderStatus.PENDING_APPROVAL)
                        .statusChangedAt(now)
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
                        .customer(customer)
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
                        .quantity(requestedQty)
                        .unitPrice(unitPrice)
                        .subTotal(subTotal)
                        .build();

                order.getOrderItems().add(newItem);
                newSubOrder.getOrderItems().add(newItem);
                order.getSubOrders().add(newSubOrder);
            }
        }

        for (SubOrderEntity subOrder : order.getSubOrders()) {
            orderSupport.recalculateSubOrderTotal(subOrder);
        }

        orderSupport.recalculateOrderTotal(order);
        order.setStatusChangedAt(now);

        subOrderReviewUseCase.evaluateAndSyncMasterOrderStatus(order, username != null ? username : "CUSTOMER", "Cập nhật đơn hàng: thêm sản phẩm/đơn con chờ phê duyệt");

        OrderEntity savedOrder = orderRepository.save(order);

        orderSupport.recordHistory(savedOrder, savedOrder.getStatus(), savedOrder.getStatus(), "CUSTOMER",
                username != null ? username : String.valueOf(customer.getId()),
                "Khách hàng mua thêm sản phẩm, tự động cập nhật và gộp vào đơn hàng đang chờ duyệt.");

        return orderMapper.toResponse(savedOrder);
    }
}
