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

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Endpoint Return URL: Tiếp nhận redirect từ trình duyệt người dùng sau khi thanh toán trên cổng VNPay.
     * Tự động chuyển hướng về trang Hóa đơn (hoặc trang Đơn hàng) trên Frontend.
     */
    @GetMapping("/return")
    public void returnUrl(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        Map<String, String> params = extractParams(request);
        Map<String, String> result = vnPayService.handleReturn(params);

        String status = result.get("status");
        String invoiceId = result.get("invoiceId");
        String orderId = result.get("orderId");

        String redirectTarget;
        if ("SUCCESS".equalsIgnoreCase(status)) {
            if (invoiceId != null && !invoiceId.isBlank()) {
                redirectTarget = frontendUrl + "/invoices/" + invoiceId + "?payment=success";
            } else {
                redirectTarget = frontendUrl + "/invoices?payment=success";
            }
        } else {
            if (orderId != null && !orderId.isBlank()) {
                redirectTarget = frontendUrl + "/orders/" + orderId + "?payment=failed";
            } else {
                redirectTarget = frontendUrl + "/orders?payment=failed";
            }
        }

        response.sendRedirect(redirectTarget);
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
