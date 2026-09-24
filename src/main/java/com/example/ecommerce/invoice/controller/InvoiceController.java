package com.example.ecommerce.invoice.controller;

import com.example.ecommerce.common.response.ApiResponse;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.invoice.dto.response.InvoiceResponse;
import com.example.ecommerce.invoice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/invoices")
@Tag(
        name = "Invoice Management",
        description = "APIs cho việc quản lý, tra cứu và tải hóa đơn (Invoice PDF)."
)
public class InvoiceController {

    private final InvoiceService invoiceService;

    @Operation(
            summary = "Lấy danh sách hóa đơn (Admin / Product Owner)",
            description = "API cho phép lấy danh sách hóa đơn toàn hệ thống, hỗ trợ lọc theo status, customerId, ngày xuất và từ khóa."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<InvoiceResponse>>> getInvoices(
            @RequestParam(required = false) Map<String, Object> params,
            @RequestParam(name = "sortBy", required = false) List<String> sortByParams
    ) {
        PageResponse<InvoiceResponse> pageResponse = invoiceService.getInvoices(params, sortByParams);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách hóa đơn thành công", pageResponse));
    }

    @Operation(
            summary = "Lấy chi tiết hóa đơn theo ID",
            description = "API lấy chi tiết hóa đơn. Khách hàng chỉ xem được hóa đơn của chính mình; Admin/PO xem được toàn bộ."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        List<String> roles = extractRoles(authentication);
        InvoiceResponse response = invoiceService.getInvoiceById(id, username, roles);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin hóa đơn thành công", response));
    }

    @Operation(
            summary = "Lấy chi tiết hóa đơn theo mã số hóa đơn (Invoice Number)",
            description = "API tra cứu hóa đơn theo định dạng mã số INV-YYYYMMDD-XXXXXX."
    )
    @GetMapping("/by-number/{invoiceNumber}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByNumber(
            @PathVariable String invoiceNumber,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        List<String> roles = extractRoles(authentication);
        InvoiceResponse response = invoiceService.getInvoiceByInvoiceNumber(invoiceNumber, username, roles);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin hóa đơn thành công", response));
    }

    @Operation(
            summary = "Tải file PDF hóa đơn",
            description = "API trả về file PDF hóa đơn dưới dạng attachment stream."
    )
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> downloadInvoicePdf(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        List<String> roles = extractRoles(authentication);
        InvoiceResponse invoice = invoiceService.getInvoiceById(id, username, roles);
        Resource pdfResource = invoiceService.getInvoicePdfResource(id, username, roles);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + invoice.getInvoiceNumber() + ".pdf\"")
                .body(pdfResource);
    }

    @Operation(
            summary = "Tạo hóa đơn cho đơn hàng đã thanh toán",
            description = "API cho phép tạo hóa đơn cho đơn hàng hợp lệ nếu chưa được tạo tự động."
    )
    @PostMapping("/orders/{orderId}/generate")
    public ResponseEntity<ApiResponse<InvoiceResponse>> generateInvoiceForOrder(@PathVariable Long orderId) {
        InvoiceResponse response = invoiceService.createInvoiceForOrderId(orderId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Phát hành hóa đơn thành công", response));
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
