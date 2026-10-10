package com.example.ecommerce.service;

import com.example.ecommerce.common.exception.*;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.order.dto.request.AddOrderItemRequest;
import com.example.ecommerce.order.dto.request.OrderItemRequest;
import com.example.ecommerce.order.dto.request.OrderRequest;
import com.example.ecommerce.order.dto.request.UpdateOrderItemRequest;
import com.example.ecommerce.order.dto.response.OrderItemsResponse;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.OrderItemEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.mapper.OrderMapper;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.order.repository.OrderStatusHistoryRepository;
import com.example.ecommerce.order.repository.SubOrderRepository;
import com.example.ecommerce.order.service.OrderItemService;
import com.example.ecommerce.order.service.impl.OrderServiceImpl;
import com.example.ecommerce.product.entity.ProductEntity;
import com.example.ecommerce.product.enums.ProductStatus;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderItemManagementTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private SubOrderRepository subOrderRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderItemService orderItemService;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Mock
    private com.example.ecommerce.auth.repository.UserRepository userRepository;
    @Mock
    private com.example.ecommerce.ticket.repository.TicketRepository ticketRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private CustomerEntity customer;
    private ProductEntity iphone;
    private ProductEntity caseProduct;
    private OrderEntity order;
    private com.example.ecommerce.order.entity.SubOrderEntity defaultSubOrder;

    @BeforeEach
    void setUp() {
        customer = CustomerEntity.builder().id(1L).name("Khach Hang A").build();

        iphone = ProductEntity.builder()
                .id(10L)
                .name("iPhone 16 128GB")
                .price(BigDecimal.valueOf(22990000))
                .stock(10)
                .status(ProductStatus.ACTIVE)
                .deleted(false)
                .build();

        caseProduct = ProductEntity.builder()
                .id(20L)
                .name("Op lung iPhone")
                .price(BigDecimal.valueOf(190000))
                .stock(100)
                .status(ProductStatus.ACTIVE)
                .deleted(false)
                .build();

        order = OrderEntity.builder()
                .id(100L)
                .customer(customer)
                .status(OrderStatus.PENDING_APPROVAL)
                .statusChangedAt(LocalDateTime.now())
                .totalAmount(BigDecimal.ZERO)
                .orderItems(new ArrayList<>())
                .subOrders(new ArrayList<>())
                .build();

        defaultSubOrder = com.example.ecommerce.order.entity.SubOrderEntity.builder()
                .id(500L)
                .masterOrder(order)
                .subOrderCode("SO-100-1")
                .status(com.example.ecommerce.order.enums.SubOrderStatus.PENDING_APPROVAL)
                .statusChangedAt(LocalDateTime.now())
                .totalAmount(BigDecimal.ZERO)
                .orderItems(new ArrayList<>())
                .build();
        order.getSubOrders().add(defaultSubOrder);
    }

    @Test
    @DisplayName("Rule 1: Chi cho phep them, sua, xoa khi order o trang thai PENDING_APPROVAL, PENDING_PO_REVIEW hoac DRAFT. Neu status khac -> 409 ORDER_NOT_EDITABLE")
    void shouldThrowOrderNotEditableWhenStatusNotPending() {
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(order));

        AddOrderItemRequest addReq = AddOrderItemRequest.builder().productId(10L).quantity(1).build();
        assertThatThrownBy(() -> orderService.addOrderItem(100L, addReq))
                .isInstanceOf(OrderNotEditableException.class);

        UpdateOrderItemRequest updateReq = UpdateOrderItemRequest.builder().quantity(2).build();
        assertThatThrownBy(() -> orderService.updateOrderItemQuantity(100L, 10L, updateReq))
                .isInstanceOf(OrderNotEditableException.class);

        assertThatThrownBy(() -> orderService.deleteOrderItem(100L, 10L))
                .isInstanceOf(OrderNotEditableException.class);
    }

    @Test
    @DisplayName("Rule 2, 7, 9, 10: Them san pham da co thi cong don so luong, khong tao item moi, tru stock, giu nguyen unitPrice")
    void shouldMergeQuantityWhenProductAlreadyInOrder() {
        OrderItemEntity initialItem = OrderItemEntity.builder()
                .id(1L)
                .order(order)
                .subOrder(defaultSubOrder)
                .product(iphone)
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(22990000))
                .subTotal(BigDecimal.valueOf(45980000))
                .build();
        defaultSubOrder.getOrderItems().add(initialItem);
        order.getOrderItems().add(initialItem);
        order.setTotalAmount(BigDecimal.valueOf(45980000));
        iphone.setStock(8); // Stock sau khi tao ban dau

        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdAndDeletedFalseForUpdate(10L)).thenReturn(Optional.of(iphone));

        // Add 3 iPhone
        AddOrderItemRequest request = AddOrderItemRequest.builder().productId(10L).quantity(3).build();
        OrderItemsResponse response = orderService.addOrderItem(100L, request);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(response.getItems().get(0).getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(114950000));
        assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(114950000));
        assertThat(iphone.getStock()).isEqualTo(5); // 8 - 3 = 5
    }

    @Test
    @DisplayName("Rule 4: quantity trong request phai tu 1 den 99, sai -> VALIDATION_ERROR")
    void shouldThrowValidationErrorWhenQuantityInvalid() {
        AddOrderItemRequest reqZero = AddOrderItemRequest.builder().productId(10L).quantity(0).build();
        assertThatThrownBy(() -> orderService.addOrderItem(100L, reqZero))
                .isInstanceOf(ValidationErrorException.class);

        AddOrderItemRequest reqExceed = AddOrderItemRequest.builder().productId(10L).quantity(100).build();
        assertThatThrownBy(() -> orderService.addOrderItem(100L, reqExceed))
                .isInstanceOf(ValidationErrorException.class);
    }

    @Test
    @DisplayName("Rule 5: quantity sau khi cong vuot 99 -> QUANTITY_LIMIT_EXCEEDED")
    void shouldThrowQuantityLimitExceededWhenTotalExceeds99() {
        OrderItemEntity initialItem = OrderItemEntity.builder()
                .id(2L)
                .order(order)
                .subOrder(defaultSubOrder)
                .product(caseProduct)
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(190000))
                .subTotal(BigDecimal.valueOf(190000))
                .build();
        defaultSubOrder.getOrderItems().add(initialItem);
        order.getOrderItems().add(initialItem);
        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdAndDeletedFalseForUpdate(20L)).thenReturn(Optional.of(caseProduct));

        AddOrderItemRequest request = AddOrderItemRequest.builder().productId(20L).quantity(99).build();
        assertThatThrownBy(() -> orderService.addOrderItem(100L, request))
                .isInstanceOf(QuantityLimitExceededException.class);
    }

    @Test
    @DisplayName("Rule 6: San pham INACTIVE -> PRODUCT_INACTIVE")
    void shouldThrowProductInactiveWhenStatusInactive() {
        iphone.setStatus(ProductStatus.INACTIVE);
        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdAndDeletedFalseForUpdate(10L)).thenReturn(Optional.of(iphone));

        AddOrderItemRequest request = AddOrderItemRequest.builder().productId(10L).quantity(1).build();
        assertThatThrownBy(() -> orderService.addOrderItem(100L, request))
                .isInstanceOf(ProductInactiveException.class);
    }

    @Test
    @DisplayName("Rule 8: Khong du stock -> 409 INSUFFICIENT_STOCK")
    void shouldThrowInsufficientStockWhenStockNotEnough() {
        iphone.setStock(8);
        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdAndDeletedFalseForUpdate(10L)).thenReturn(Optional.of(iphone));

        // Yeu cau 9, ton kho 8
        AddOrderItemRequest request = AddOrderItemRequest.builder().productId(10L).quantity(9).build();
        assertThatThrownBy(() -> orderService.addOrderItem(100L, request))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(iphone.getStock()).isEqualTo(8); // Du lieu giu nguyen
    }

    @Test
    @DisplayName("PATCH /items: Dieu chinh stock theo chenh lech delta")
    void shouldUpdateQuantityAndAdjustStockCorrectly() {
        OrderItemEntity initialItem = OrderItemEntity.builder()
                .id(1L)
                .order(order)
                .subOrder(defaultSubOrder)
                .product(iphone)
                .quantity(5)
                .unitPrice(BigDecimal.valueOf(22990000))
                .subTotal(BigDecimal.valueOf(114950000))
                .build();
        defaultSubOrder.getOrderItems().add(initialItem);
        order.getOrderItems().add(initialItem);
        order.setTotalAmount(BigDecimal.valueOf(114950000));
        iphone.setStock(5); // Stock hien tai

        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdAndDeletedFalseForUpdate(10L)).thenReturn(Optional.of(iphone));

        // PATCH quantity giam xuong 2: delta = 2 - 5 = -3 -> hoan 3 vao stock -> stock thanh 8
        UpdateOrderItemRequest request = UpdateOrderItemRequest.builder().quantity(2).build();
        OrderItemsResponse response = orderService.updateOrderItemQuantity(100L, 10L, request);

        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(response.getItems().get(0).getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(45980000));
        assertThat(iphone.getStock()).isEqualTo(8);
    }

    @Test
    @DisplayName("DELETE /items: Xoa item khoi don hang va hoan stock")
    void shouldDeleteItemAndRestoreStock() {
        OrderItemEntity iphoneItem = OrderItemEntity.builder()
                .id(1L)
                .order(order)
                .subOrder(defaultSubOrder)
                .product(iphone)
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(22990000))
                .subTotal(BigDecimal.valueOf(45980000))
                .build();
        defaultSubOrder.getOrderItems().add(iphoneItem);
        OrderItemEntity caseItem = OrderItemEntity.builder()
                .id(2L)
                .order(order)
                .subOrder(defaultSubOrder)
                .product(caseProduct)
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(190000))
                .subTotal(BigDecimal.valueOf(190000))
                .build();
        defaultSubOrder.getOrderItems().add(caseItem);
        order.getOrderItems().add(iphoneItem);
        order.getOrderItems().add(caseItem);
        order.setTotalAmount(BigDecimal.valueOf(46170000));
        caseProduct.setStock(99);

        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdAndDeletedFalseForUpdate(20L)).thenReturn(Optional.of(caseProduct));

        OrderItemsResponse response = orderService.deleteOrderItem(100L, 20L);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductId()).isEqualTo(10L);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(45980000));
        assertThat(caseProduct.getStock()).isEqualTo(100); // 99 + 1 = 100
    }

    @Test
    @DisplayName("Draft Order: Nguoi dung co the them/sua/xoa item tren don DRAFT ma khong tru/hoan stock")
    void shouldAllowEditingWhenOrderIsDraftWithoutModifyingStock() {
        order.setStatus(OrderStatus.DRAFT);
        iphone.setStock(10);
        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdAndDeletedFalseForUpdate(10L)).thenReturn(Optional.of(iphone));

        // Add item to draft
        AddOrderItemRequest addReq = AddOrderItemRequest.builder().productId(10L).quantity(2).build();
        OrderItemsResponse addRes = orderService.addOrderItem(100L, addReq);
        assertThat(addRes.getItems()).hasSize(1);
        assertThat(iphone.getStock()).isEqualTo(10); // Khong tru stock trong DRAFT

        // Update item in draft
        UpdateOrderItemRequest updateReq = UpdateOrderItemRequest.builder().quantity(5).build();
        OrderItemsResponse updateRes = orderService.updateOrderItemQuantity(100L, 10L, updateReq);
        assertThat(updateRes.getItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(iphone.getStock()).isEqualTo(10); // Khong doi stock trong DRAFT

        // Delete item in draft
        OrderItemsResponse delRes = orderService.deleteOrderItem(100L, 10L);
        assertThat(delRes.getItems()).isEmpty();
        assertThat(iphone.getStock()).isEqualTo(10); // Khong hoan stock trong DRAFT
    }

    @Test
    @DisplayName("Auto-merge: createOrder tu dong lay don PENDING_APPROVAL hien co va cong don so luong san pham")
    void shouldAutoMergeIntoExistingPendingApprovalOrderWhenCreateOrderCalled() {
        OrderItemEntity initialItem = OrderItemEntity.builder()
                .id(1L)
                .order(order)
                .subOrder(defaultSubOrder)
                .product(iphone)
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(22990000))
                .subTotal(BigDecimal.valueOf(45980000))
                .build();
        defaultSubOrder.getOrderItems().add(initialItem);
        order.getOrderItems().add(initialItem);
        order.setTotalAmount(BigDecimal.valueOf(45980000));

        when(customerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(customer));
        when(orderRepository.findByCustomerIdAndStatusInForUpdate(eq(1L), anyList()))
                .thenReturn(List.of(order));

        OrderRequest request = new OrderRequest();
        request.setCustomerId(1L);
        OrderItemRequest req1 = new OrderItemRequest();
        req1.setProductId(10L);
        req1.setQuantity(3); // Buy 3 more iPhones
        request.setOrderItems(List.of(req1));

        Map<Long, Integer> mergedQuantities = Map.of(10L, 3);
        when(orderItemService.mergeRequestedQuantities(request.getOrderItems())).thenReturn(mergedQuantities);
        when(orderItemService.validateAndLoadProductsForUpdate(mergedQuantities)).thenReturn(Map.of(10L, iphone));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(OrderResponse.builder().id(100L).status(OrderStatus.PENDING_APPROVAL).build());

        OrderResponse response = orderService.createOrder(request, "customer1");

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(initialItem.getQuantity()).isEqualTo(5); // 2 + 3 = 5
        assertThat(initialItem.getSubTotal()).isEqualByComparingTo(BigDecimal.valueOf(114950000));
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(114950000));
        verify(orderItemService).decrementStock(anyMap(), eq(mergedQuantities));
    }

    @Test
    @DisplayName("SubOrder APPROVED: Khi them san pham / tang so luong, suborder da duyet giu nguyen va phai tao 1 ordersub con khac cho PO duyet")
    void shouldCreateNewSubOrderWhenExistingSubOrderIsApproved() {
        order.getSubOrders().clear(); // Clear defaultSubOrder de chi con approvedSubOrder
        com.example.ecommerce.order.entity.SubOrderEntity approvedSubOrder = com.example.ecommerce.order.entity.SubOrderEntity.builder()
                .id(501L)
                .masterOrder(order)
                .subOrderCode("SO-100-1")
                .status(com.example.ecommerce.order.enums.SubOrderStatus.APPROVED)
                .statusChangedAt(LocalDateTime.now().minusHours(1))
                .approvedAt(LocalDateTime.now().minusHours(1))
                .reviewNotes("PO da duyet ban dau")
                .totalAmount(BigDecimal.valueOf(45980000))
                .orderItems(new ArrayList<>())
                .build();

        OrderItemEntity initialItem = OrderItemEntity.builder()
                .id(1L)
                .order(order)
                .subOrder(approvedSubOrder)
                .product(iphone)
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(22990000))
                .subTotal(BigDecimal.valueOf(45980000))
                .build();
        approvedSubOrder.getOrderItems().add(initialItem);
        order.getOrderItems().add(initialItem);
        order.getSubOrders().add(approvedSubOrder);
        order.setStatus(OrderStatus.PENDING_PO_REVIEW);

        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdAndDeletedFalseForUpdate(10L)).thenReturn(Optional.of(iphone));
        when(subOrderRepository.save(any(com.example.ecommerce.order.entity.SubOrderEntity.class))).thenAnswer(invocation -> {
            com.example.ecommerce.order.entity.SubOrderEntity so = invocation.getArgument(0);
            so.setId(502L);
            return so;
        });

        AddOrderItemRequest addReq = AddOrderItemRequest.builder().productId(10L).quantity(1).build();
        OrderItemsResponse response = orderService.addOrderItem(100L, addReq);

        // 1. SubOrder cu da duyet van GIU NGUYEN la APPROVED va quantity la 2
        assertThat(approvedSubOrder.getStatus()).isEqualTo(com.example.ecommerce.order.enums.SubOrderStatus.APPROVED);
        assertThat(initialItem.getQuantity()).isEqualTo(2);

        // 2. Tao 1 SubOrder moi o trang thai PENDING_APPROVAL
        assertThat(order.getSubOrders()).hasSize(2);
        com.example.ecommerce.order.entity.SubOrderEntity newSubOrder = order.getSubOrders().get(1);
        assertThat(newSubOrder.getStatus()).isEqualTo(com.example.ecommerce.order.enums.SubOrderStatus.PENDING_APPROVAL);
        assertThat(newSubOrder.getOrderItems()).hasSize(1);
        assertThat(newSubOrder.getOrderItems().get(0).getQuantity()).isEqualTo(1);

        // 3. Ticket moi duoc tao cho SubOrder moi
        verify(ticketRepository).save(any(com.example.ecommerce.ticket.entity.PurchaseTicketEntity.class));
    }

    @Test
    @DisplayName("SubOrder PENDING_APPROVAL: Khi them san pham da co thi cong truc tiep so luong vao don con cho duyet")
    void shouldDirectlyAddQuantityWhenSubOrderIsPendingApproval() {
        order.getSubOrders().clear();
        com.example.ecommerce.order.entity.SubOrderEntity pendingSubOrder = com.example.ecommerce.order.entity.SubOrderEntity.builder()
                .id(501L)
                .masterOrder(order)
                .subOrderCode("SO-100-1")
                .status(com.example.ecommerce.order.enums.SubOrderStatus.PENDING_APPROVAL)
                .statusChangedAt(LocalDateTime.now())
                .totalAmount(BigDecimal.valueOf(45980000))
                .orderItems(new ArrayList<>())
                .build();

        OrderItemEntity initialItem = OrderItemEntity.builder()
                .id(1L)
                .order(order)
                .subOrder(pendingSubOrder)
                .product(iphone)
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(22990000))
                .subTotal(BigDecimal.valueOf(45980000))
                .build();
        pendingSubOrder.getOrderItems().add(initialItem);
        order.getOrderItems().add(initialItem);
        order.getSubOrders().add(pendingSubOrder);
        order.setStatus(OrderStatus.PENDING_APPROVAL);

        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdAndDeletedFalseForUpdate(10L)).thenReturn(Optional.of(iphone));

        AddOrderItemRequest addReq = AddOrderItemRequest.builder().productId(10L).quantity(1).build();
        OrderItemsResponse response = orderService.addOrderItem(100L, addReq);

        // 1. Cong truc tiep so luong vao item hien co: 2 + 1 = 3
        assertThat(initialItem.getQuantity()).isEqualTo(3);
        // 2. Khong tao them suborder moi
        assertThat(order.getSubOrders()).hasSize(1);
        assertThat(pendingSubOrder.getStatus()).isEqualTo(com.example.ecommerce.order.enums.SubOrderStatus.PENDING_APPROVAL);
    }
}
