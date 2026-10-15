package com.example.ecommerce.service;

import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.payment.entity.PaymentEntity;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.payment.enums.PaymentMethod;
import com.example.ecommerce.payment.enums.PaymentStatus;
import com.example.ecommerce.common.exception.OrderProcessingException;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.order.service.OrderService;
import com.example.ecommerce.payment.vnpay.config.VnPayConfig;
import com.example.ecommerce.payment.repository.PaymentRepository;
import com.example.ecommerce.payment.vnpay.service.impl.VnPayServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VnPayServiceImplTest {

    @Mock
    private VnPayConfig vnPayConfig;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderService orderService;
    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private VnPayServiceImpl vnPayService;

    @Test
    @DisplayName("createPaymentUrl should reuse pending payment by order id")
    void createPaymentUrlShouldReusePendingPayment() {
        OrderEntity order = new OrderEntity();
        order.setId(10L);
        order.setStatus(OrderStatus.AWAITING_PAYMENT);
        order.setTotalAmount(BigDecimal.valueOf(125_000));

        PaymentEntity payment = new PaymentEntity();
        payment.setId(99L);
        payment.setOrder(order);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setTotalAmount(order.getTotalAmount());
        payment.setTransactionRef("ORD10_EXISTING");

        mockRequestAndConfig();
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(payment));
        when(paymentRepository.saveAndFlush(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String paymentUrl = vnPayService.createPaymentUrl(10L, httpServletRequest);

        assertThat(paymentUrl).contains("vnp_TxnRef=ORD10_EXISTING");
    }

    @Test
    @DisplayName("createPaymentUrl should fail when order was already paid")
    void createPaymentUrlShouldFailWhenAlreadyPaid() {
        OrderEntity order = new OrderEntity();
        order.setId(11L);
        order.setStatus(OrderStatus.AWAITING_PAYMENT);
        order.setTotalAmount(BigDecimal.valueOf(50_000));

        PaymentEntity payment = new PaymentEntity();
        payment.setOrder(order);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setTransactionRef("ORD11_PAID");

        when(orderRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(11L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> vnPayService.createPaymentUrl(11L, httpServletRequest))
                .isInstanceOf(OrderProcessingException.class)
                .hasMessageContaining("already paid successfully");
    }

    private void mockRequestAndConfig() {
        when(vnPayConfig.getTmnCode()).thenReturn("TMNCODE");
        when(vnPayConfig.getHashSecret()).thenReturn("SECRET");
        when(vnPayConfig.getPaymentUrl()).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        when(vnPayConfig.getReturnUrl()).thenReturn("https://example.com/payments/vnpay/return");
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpServletRequest.getHeader("X-Real-IP")).thenReturn(null);
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
    }
}
