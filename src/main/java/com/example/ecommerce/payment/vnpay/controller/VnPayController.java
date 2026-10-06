package com.example.ecommerce.payment.vnpay.controller;

import com.example.ecommerce.payment.dto.response.PaymentResponse;
import com.example.ecommerce.payment.vnpay.dto.request.VnPayPaymentRequest;
import com.example.ecommerce.payment.vnpay.dto.response.VnPayPaymentResponse;
import com.example.ecommerce.payment.service.PaymentService;
import com.example.ecommerce.payment.vnpay.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payments/vnpay")
@RequiredArgsConstructor
public class VnPayController {

    private final VnPayService vnPayService;
    private final PaymentService paymentService;

    /**
     * Tạo URL thanh toán VNPay và trả về thông tin giao dịch cho client để chuyển hướng.
     */
    @PostMapping
    public ResponseEntity<VnPayPaymentResponse> createPayment(@Valid @RequestBody VnPayPaymentRequest request, HttpServletRequest httpRequest) {
        String paymentUrl = vnPayService.createPaymentUrl(request.getOrderId(), httpRequest);
        PaymentResponse payment = paymentService.getPaymentByOrderId(request.getOrderId());
        return ResponseEntity.ok(VnPayPaymentResponse.builder()
                .paymentId(payment.getId())
                .transactionRef(payment.getTransactionRef())
                .paymentUrl(paymentUrl)
                .build());
    }

    /**
     * Endpoint IPN (Instant Payment Notification): Nhận webhook ngầm server-to-server từ VNPay khi giao dịch hoàn tất.
     */
    @GetMapping("/ipn")
    public ResponseEntity<?> ipn(HttpServletRequest request) {
        Map<String, String> params = extractParams(request);
        String result = vnPayService.handleIpn(params);
        return ResponseEntity.ok(result);
    }

    /**
     * Endpoint Return URL: Tiếp nhận redirect từ trình duyệt người dùng sau khi thanh toán trên cổng VNPay.
     */
    @GetMapping("/return")
    public ResponseEntity<?> returnUrl(HttpServletRequest request) {
        Map<String, String> params = extractParams(request);
        Map<String, String> result = vnPayService.handleReturn(params);
        return ResponseEntity.ok(result);
    }

    /**
     * Trích xuất toàn bộ query parameters từ HttpServletRequest thành Map<String, String>.
     */
    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String fieldName = parameterNames.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && !fieldValue.isBlank()) {
                params.put(fieldName, fieldValue);
            }
        }

        return params;
    }
}
