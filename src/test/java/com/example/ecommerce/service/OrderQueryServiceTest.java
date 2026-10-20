package com.example.ecommerce.service;

import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.customer.repository.CustomerRepository;
import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.order.dto.response.OrderStatusHistoryResponse;
import com.example.ecommerce.order.dto.response.SubOrderResponse;
import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.order.entity.OrderStatusHistoryEntity;
import com.example.ecommerce.order.entity.SubOrderEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.enums.SubOrderStatus;
import com.example.ecommerce.order.mapper.OrderMapper;
import com.example.ecommerce.order.mapper.SubOrderMapper;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.order.repository.OrderStatusHistoryRepository;
import com.example.ecommerce.order.repository.SubOrderRepository;
import com.example.ecommerce.order.service.impl.OrderQueryServiceImpl;
import com.example.ecommerce.testdata.CustomerTestDataBuilder;
import com.example.ecommerce.testdata.OrderTestDataBuilder;
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
class OrderQueryServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SubOrderRepository subOrderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private SubOrderMapper subOrderMapper;

    @InjectMocks
    private OrderQueryServiceImpl orderQueryService;

    private CustomerEntity customer;

    @BeforeEach
    void setUp() {
        customer = CustomerTestDataBuilder.aCustomer().withId(1L).build();
    }

    @Test
    @DisplayName("getOrderById should return mapped order when found")
    void getOrderByIdShouldReturnOrder() {
        OrderEntity order = OrderTestDataBuilder.anOrder().withId(10L).withCustomer(customer).build();
        OrderResponse response = OrderResponse.builder().id(10L).customerId(1L).status(OrderStatus.AWAITING_PAYMENT).build();

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(response);

        OrderResponse result = orderQueryService.getOrderById(10L);
        assertThat(result.getId()).isEqualTo(10L);
        verify(orderRepository).findById(10L);
    }

    @Test
    @DisplayName("getOrderById should throw ResourceNotFoundException when not found")
    void getOrderByIdShouldThrowWhenNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderQueryService.getOrderById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order with id 99 not found.");
    }

    @Test
    @DisplayName("getSubOrderById should return sub order when found")
    void getSubOrderByIdShouldReturnSubOrder() {
        SubOrderEntity subOrder = SubOrderEntity.builder().id(20L).subOrderCode("SO-001").status(SubOrderStatus.PENDING_APPROVAL).build();
        SubOrderResponse response = SubOrderResponse.builder().id(20L).subOrderCode("SO-001").status(SubOrderStatus.PENDING_APPROVAL).build();

        when(subOrderRepository.findById(20L)).thenReturn(Optional.of(subOrder));
        when(subOrderMapper.toResponse(subOrder)).thenReturn(response);

        SubOrderResponse result = orderQueryService.getSubOrderById(20L);
        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getSubOrderCode()).isEqualTo("SO-001");
    }

    @Test
    @DisplayName("getSubOrderById should throw when not found")
    void getSubOrderByIdShouldThrowWhenNotFound() {
        when(subOrderRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderQueryService.getSubOrderById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getOrders should return paged orders with filters")
    void getOrdersShouldReturnPagedOrders() {
        OrderEntity order = OrderTestDataBuilder.anOrder().withId(1L).withCustomer(customer).build();
        OrderResponse mapped = OrderResponse.builder().id(1L).customerId(1L).status(OrderStatus.AWAITING_PAYMENT).build();

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(order)));
        when(orderMapper.toResponse(order)).thenReturn(mapped);

        PageResponse<OrderResponse> result = orderQueryService.getOrders(
                Map.of("status", "AWAITING_PAYMENT", "minTotalAmount", "10000", "page", "0", "size", "10"),
                List.of("createdAt,desc")
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getOrders should throw BadRequestException on invalid date range")
    void getOrdersShouldThrowOnInvalidDateRange() {
        assertThatThrownBy(() -> orderQueryService.getOrders(Map.of(
                "createdFrom", "2026-10-30",
                "createdTo", "2026-10-01"
        )))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("createdFrom phải nhỏ hơn hoặc bằng createdTo.");
    }

    @Test
    @DisplayName("getOrdersByCustomerId should return paged orders for customer")
    void getOrdersByCustomerIdShouldReturnPage() {
        OrderEntity order = OrderTestDataBuilder.anOrder().withId(5L).withCustomer(customer).build();
        OrderResponse mapped = OrderResponse.builder().id(5L).customerId(1L).status(OrderStatus.PAID).build();

        when(customerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(customer));
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(order)));
        when(orderMapper.toResponse(order)).thenReturn(mapped);

        PageResponse<OrderResponse> result = orderQueryService.getOrdersByCustomerId(1L, Map.of(), null);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getOrderStatusHistory should return history ordered by createdAt")
    void getOrderStatusHistoryShouldReturnList() {
        OrderStatusHistoryEntity h1 = OrderStatusHistoryEntity.builder()
                .id(1L)
                .fromStatus(null)
                .toStatus(OrderStatus.PENDING_APPROVAL)
                .actorType("CUSTOMER")
                .build();
        h1.setCreatedAt(LocalDateTime.now().minusHours(2));

        OrderStatusHistoryEntity h2 = OrderStatusHistoryEntity.builder()
                .id(2L)
                .fromStatus(OrderStatus.PENDING_APPROVAL)
                .toStatus(OrderStatus.AWAITING_PAYMENT)
                .actorType("PRODUCT_OWNER")
                .build();
        h2.setCreatedAt(LocalDateTime.now().minusHours(1));

        when(orderStatusHistoryRepository.findAllByOrderIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(h1, h2));

        List<OrderStatusHistoryResponse> result = orderQueryService.getOrderStatusHistory(10L);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getToStatus()).isEqualTo(OrderStatus.PENDING_APPROVAL);
        assertThat(result.get(1).getToStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
    }
}
