package com.example.ecommerce.payment.vnpay.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public interface VnPayService {
    String createPaymentUrl(Long orderId, HttpServletRequest request);
    Map<String, String> handleReturn(Map<String, String> params);
    String handleIpn(Map<String, String> params);
    Map<String, String> refund(com.example.ecommerce.payment.entity.PaymentEntity payment, BigDecimal amount, String reason, String actorIdentifier);
}
