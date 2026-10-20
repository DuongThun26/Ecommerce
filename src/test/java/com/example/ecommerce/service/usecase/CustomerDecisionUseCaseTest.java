package com.example.ecommerce.service.usecase;

import com.example.ecommerce.common.exception.OrderProcessingException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.customer.entity.CustomerEntity;
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
import com.example.ecommerce.order.service.usecase.CustomerDecisionUseCase;
import com.example.ecommerce.payment.repository.PaymentRepository;
import com.example.ecommerce.ticket.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerDecisionUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SubOrderRepository subOrderRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderSupport orderSupport;

    @InjectMocks
    private CustomerDecisionUseCase customerDecisionUseCase;

    private OrderEntity order;

    @BeforeEach
    void setUp() {
        order = OrderEntity.builder()
                .id(1L)
                .status(OrderStatus.ACTION_REQUIRED)
                .totalAmount(BigDecimal.valueOf(100000))
                .build();
    }

    @Test
    @DisplayName("handleCustomerDecision should throw when order status is not ACTION_REQUIRED")
    void handleCustomerDecisionShouldThrowWhenNotActionRequired() {
        order.setStatus(OrderStatus.AWAITING_PAYMENT);
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));

        CustomerDecisionRequest request = new CustomerDecisionRequest();
        request.setAction(CustomerDecisionAction.ACCEPT_PARTIAL);

        assertThatThrownBy(() -> customerDecisionUseCase.handleCustomerDecision(1L, request, "customer1"))
                .isInstanceOf(OrderProcessingException.class)
                .hasMessageContaining("Chỉ đơn hàng ở trạng thái ACTION_REQUIRED");
    }

    @Test
    @DisplayName("handleCustomerDecision with CANCEL_ALL should mark order CANCELLED")
    void handleCustomerDecisionWithCancelAllShouldCancelOrder() {
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
        when(subOrderRepository.findByMasterOrderId(1L)).thenReturn(List.of());
        when(ticketRepository.findByOrderId(1L)).thenReturn(List.of());
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderMapper.toResponse(any(OrderEntity.class))).thenReturn(OrderResponse.builder().id(1L).status(OrderStatus.CANCELLED).build());

        CustomerDecisionRequest request = new CustomerDecisionRequest();
        request.setAction(CustomerDecisionAction.CANCEL_ALL);
        request.setNotes("Too many rejected items");

        OrderResponse response = customerDecisionUseCase.handleCustomerDecision(1L, request, "customer1");

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderSupport).recordHistory(any(), any(), org.mockito.ArgumentMatchers.eq(OrderStatus.CANCELLED), any(), any(), any());
    }

    @Test
    @DisplayName("handleCustomerDecision with ACCEPT_PARTIAL should update order to AWAITING_PAYMENT with recalculated total")
    void handleCustomerDecisionWithAcceptPartialShouldAdvanceToAwaitingPayment() {
        SubOrderEntity approvedSub = SubOrderEntity.builder()
                .id(10L)
                .status(SubOrderStatus.APPROVED)
                .totalAmount(BigDecimal.valueOf(60000))
                .build();
        SubOrderEntity rejectedSub = SubOrderEntity.builder()
                .id(11L)
                .status(SubOrderStatus.REJECTED)
                .totalAmount(BigDecimal.valueOf(40000))
                .build();

        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
        when(subOrderRepository.findByMasterOrderId(1L)).thenReturn(List.of(approvedSub, rejectedSub));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderMapper.toResponse(any(OrderEntity.class))).thenReturn(OrderResponse.builder().id(1L).status(OrderStatus.AWAITING_PAYMENT).build());

        CustomerDecisionRequest request = new CustomerDecisionRequest();
        request.setAction(CustomerDecisionAction.ACCEPT_PARTIAL);

        OrderResponse response = customerDecisionUseCase.handleCustomerDecision(1L, request, "customer1");

        assertThat(response.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
        assertThat(order.getTotalAmount()).isEqualTo(BigDecimal.valueOf(60000));
        assertThat(rejectedSub.getStatus()).isEqualTo(SubOrderStatus.CANCELLED);
        verify(orderSupport).restoreStockForSubOrder(rejectedSub);
    }
}
