package com.example.ecommerce.invoice.controller;

import com.example.ecommerce.common.response.ApiResponse;
import com.example.ecommerce.invoice.dto.response.InvoiceResponse;
import com.example.ecommerce.invoice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
@Tag(
        name = "Order Invoice",
        description = "APIs tra cứu hóa đơn theo đơn hàng."
)
public class OrderInvoiceController {

    private final InvoiceService invoiceService;

    @Operation(
            summary = "Lấy hóa đơn theo Order ID",
            description = "API tra cứu hóa đơn tương ứng với một đơn hàng đã thanh toán."
    )
    @GetMapping("/{orderId}/invoice")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByOrderId(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        List<String> roles = extractRoles(authentication);
        InvoiceResponse invoice = invoiceService.getInvoiceByOrderId(orderId, username, roles);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin hóa đơn theo đơn hàng thành công", invoice));
    }

    private List<String> extractRoles(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return List.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
