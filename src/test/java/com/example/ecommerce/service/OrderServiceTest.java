package com.example.ecommerce.service;

import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.order.dto.request.OrderItemRequest;
import com.example.ecommerce.order.dto.request.OrderRequest;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.OrderItemEntity;
import com.example.ecommerce.order.service.OrderItemService;
import com.example.ecommerce.product.entity.ProductEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.common.exception.OrderProcessingException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.order.mapper.OrderMapper;
import com.example.ecommerce.customer.repository.CustomerRepository;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.order.repository.OrderStatusHistoryRepository;
import com.example.ecommerce.order.service.impl.OrderServiceImpl;
import com.example.ecommerce.testdata.CustomerTestDataBuilder;
import com.example.ecommerce.testdata.OrderTestDataBuilder;
import com.example.ecommerce.testdata.ProductTestDataBuilder;
import com.example.ecommerce.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private OrderItemService orderItemService;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Mock
    private com.example.ecommerce.auth.repository.UserRepository userRepository;
    @Mock
    private com.example.ecommerce.ticket.repository.TicketRepository ticketRepository;
    @Mock
    private com.example.ecommerce.invoice.service.InvoiceService invoiceService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private CustomerEntity customer;

    @BeforeEach
    void setUp() {
        customer = CustomerTestDataBuilder.aCustomer().withId(1L).build();
    }

    @Test
    @DisplayName("getOrderById should throw when not found")
    void getOrderByIdShouldThrowWhenNotFound() {
        when(orderRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.getOrderById(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createOrder should compose order items and total")
    void createOrderShouldComposeOrder() {
        OrderRequest request = new OrderRequest();
        request.setCustomerId(1L);
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(100L);
        item.setQuantity(2);
        request.setOrderItems(List.of(item));

        ProductEntity product = ProductTestDataBuilder.aProduct().withId(100L).withPrice(BigDecimal.valueOf(50_000)).withStock(10).build();
        OrderEntity savedOrder = OrderTestDataBuilder.anOrder().withId(5L).withCustomer(customer).withStatus(OrderStatus.PAYMENT_PENDING).build();
        OrderResponse mapped = OrderResponse.builder().id(5L).customerId(1L).status(OrderStatus.PAYMENT_PENDING).build();

        when(customerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(customer));
        when(orderItemService.mergeRequestedQuantities(any())).thenReturn(Map.of(100L, 2));
        when(orderItemService.validateAndLoadProductsForUpdate(any())).thenReturn(Map.of(100L, product));
        when(orderItemService.createOrderItems(any(), any(), any())).thenReturn(List.of(new OrderItemEntity()));
        when(orderItemService.calculateTotalAmount(any())).thenReturn(BigDecimal.valueOf(100_000));
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);
        when(orderStatusHistoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(savedOrder)).thenReturn(mapped);

        OrderResponse result = orderService.createOrder(request);

        assertThat(result.getId()).isEqualTo(5L);
        verify(orderItemService).decrementStock(any(), any());
    }

    @Test
    @DisplayName("updateOrderStatus should reject invalid transition")
    void updateOrderStatusShouldRejectInvalidTransition() {
        OrderEntity order = OrderTestDataBuilder.anOrder().withStatus(OrderStatus.COMPLETED).build();
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.PAYMENT_PENDING))
                .isInstanceOf(OrderProcessingException.class);
    }

    @Test
    @DisplayName("getOrders should reject invalid date range")
    void getOrdersShouldRejectInvalidDateRange() {
        assertThatThrownBy(() -> orderService.getOrders(Map.of(
                        "createdFrom", "2026-09-30",
                        "createdTo", "2026-09-01"
                )))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("createdFrom");
    }

    @Test
    @DisplayName("getOrdersByCustomerId should return filtered page when customer exists")
    void getOrdersByCustomerIdShouldReturnFilteredPage() {
        OrderEntity order = OrderTestDataBuilder.anOrder().withId(5L).withCustomer(customer).build();
        OrderResponse mapped = OrderResponse.builder().id(5L).customerId(1L).status(OrderStatus.PAYMENT_PENDING).build();

        when(customerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(customer));
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(order)));
        when(orderMapper.toResponse(order)).thenReturn(mapped);

        PageResponse<OrderResponse> result = orderService.getOrdersByCustomerId(
                1L,
                Map.of("status", "PAYMENT_PENDING", "keyword", "Test"),
                List.of("createdAt,desc")
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(5L);
        verify(customerRepository).findByIdAndDeletedFalse(1L);
        verify(orderRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("cancelExpiredOrders should mark pending expired orders as cancelled")
    void cancelExpiredOrdersShouldMarkCancelled() {
        ProductEntity product = ProductTestDataBuilder.aProduct().withStock(3).build();
        OrderItemEntity orderItem = new OrderItemEntity();
        orderItem.setProduct(product);
        orderItem.setQuantity(2);

        OrderEntity expired = OrderTestDataBuilder.anOrder()
                .withStatus(OrderStatus.PAYMENT_PENDING)
                .withOrderItems(List.of(orderItem))
                .withCreatedAt(LocalDateTime.now().minusDays(2))
                .withStatusChangedAt(LocalDateTime.now().minusDays(2))
                .withPaymentExpiresAt(LocalDateTime.now().minusHours(1))
                .build();
        when(orderRepository.findByStatusAndPaymentExpiresAtBefore(any(OrderStatus.class), any(LocalDateTime.class)))
                .thenReturn(List.of(expired));

        orderService.cancelExpiredOrders();

        assertThat(expired.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.getStock()).isEqualTo(5);
    }
}
