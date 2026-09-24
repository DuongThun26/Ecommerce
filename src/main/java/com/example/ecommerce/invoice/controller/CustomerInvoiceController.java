package com.example.ecommerce.invoice.controller;

import com.example.ecommerce.common.response.ApiResponse;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.invoice.dto.response.InvoiceResponse;
import com.example.ecommerce.invoice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/customers")
@Tag(
        name = "Customer Invoices",
        description = "APIs danh sách hóa đơn theo khách hàng."
)
public class CustomerInvoiceController {

    private final InvoiceService invoiceService;

    @Operation(
            summary = "Lấy danh sách hóa đơn theo Customer ID",
            description = "API cho phép xem lịch sử hóa đơn của khách hàng."
    )
    @GetMapping("/{customerId}/invoices")
    public ResponseEntity<ApiResponse<PageResponse<InvoiceResponse>>> getCustomerInvoices(
            @PathVariable Long customerId,
            @RequestParam(required = false) Map<String, Object> params,
            @RequestParam(name = "sortBy", required = false) List<String> sortByParams,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        List<String> roles = extractRoles(authentication);
        PageResponse<InvoiceResponse> pageResponse = invoiceService.getInvoicesByCustomerId(customerId, params, sortByParams, username, roles);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách hóa đơn của khách hàng thành công", pageResponse));
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
