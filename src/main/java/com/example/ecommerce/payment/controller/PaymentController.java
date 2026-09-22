package com.example.ecommerce.payment.controller;

import com.example.ecommerce.common.response.ApiResponse;
import com.example.ecommerce.payment.dto.response.PaymentResponse;
import com.example.ecommerce.payment.dto.response.PaymentStatusResponse;
import com.example.ecommerce.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/payments")
@Tag(name = "Payment", description = "API cho việc quản lý thanh toán.")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{id}")
    @Operation(
            summary = "Laays thông tin thanh toán.",
            description = "API cho phép lấy thông tin thanh toán theo id."
    )
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable Long id) {
        log.info("Lấy thông tin thanh toán theo id.");
        PaymentResponse payment = paymentService.getPayment(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy thông tin thanh toán thành công", payment));
    }

    @GetMapping("/{paymentId}/status")
    @Operation(
            summary = "Lấy trạng thái thanh toán.",
            description = "API cho phép lấy trạng thái thanh toán theo id."
    )
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatus(@PathVariable Long paymentId){
        log.info("Lấy trạng thái thanh toán theo id.");
        PaymentStatusResponse paymentStatus = paymentService.getStatus(paymentId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy trạng thái thanh toán thành công", paymentStatus));
    }
}
