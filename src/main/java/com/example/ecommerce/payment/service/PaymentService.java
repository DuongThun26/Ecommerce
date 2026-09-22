package com.example.ecommerce.payment.service;

import com.example.ecommerce.payment.dto.response.PaymentResponse;
import com.example.ecommerce.payment.dto.response.PaymentStatusResponse;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@Service
public interface PaymentService {
    PaymentResponse  getPayment(Long paymentId);
    PaymentResponse getPaymentByOrderId(Long orderId);
    PaymentStatusResponse getStatus(Long paymentId);
}
