package com.example.ecommerce.vnpay.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public interface VnPayService {
    String createPaymentUrl(Long orderId, HttpServletRequest request);
    Map<String, String> handleReturn(Map<String, String> params);
    String handleIpn(Map<String, String> params);
}
