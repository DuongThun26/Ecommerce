package com.example.ecommerce.payment.service.impl;

import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.payment.dto.response.PaymentResponse;
import com.example.ecommerce.payment.dto.response.PaymentStatusResponse;
import com.example.ecommerce.payment.entity.PaymentEntity;
import com.example.ecommerce.payment.mapper.PaymentMapper;
import com.example.ecommerce.payment.repository.PaymentRepository;
import com.example.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    @Override
    public PaymentResponse getPayment(Long paymentId) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));
        return paymentMapper.toResponse(payment);
    }

    @Override
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        PaymentEntity payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with order id: " + orderId));
        return paymentMapper.toResponse(payment);
    }

    @Override
    public PaymentStatusResponse getStatus(Long paymentId) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));
        return PaymentStatusResponse.builder()
                .status(payment.getStatus())
                .build();
    }
}
