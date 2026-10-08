package com.example.ecommerce.mail.controller;

import com.example.ecommerce.common.response.ApiResponse;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.mail.dto.request.EmailResendRequest;
import com.example.ecommerce.mail.dto.response.EmailLogResponse;
import com.example.ecommerce.mail.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/email-logs")
@Tag(
        name = "Email & Dead Letter Queue Management",
        description = "APIs quản lý hàng đợi email, lịch sử retry và Dead Letter Queue (DLQ) dành cho Admin."
)
public class EmailDlqController {

    private final EmailService emailService;

    @Operation(
            summary = "Lấy danh sách nhật ký gửi email",
            description = "API cho phép Admin tra cứu toàn bộ email, lọc theo status (PENDING, RETRYING, SENT, DEAD_LETTER), người nhận, ngày tạo."
    )
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCT_OWNER')")
    public ResponseEntity<ApiResponse<PageResponse<EmailLogResponse>>> getEmailLogs(
            @RequestParam(required = false) Map<String, Object> params,
            @RequestParam(name = "sortBy", required = false) List<String> sortByParams
    ) {
        PageResponse<EmailLogResponse> logs = emailService.getEmailLogs(params, sortByParams);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy danh sách nhật ký email thành công", logs));
    }

    @Operation(
            summary = "Lấy danh sách Dead Letter Queue (DLQ)",
            description = "API cho phép Admin tra cứu các email đã thất bại hoàn toàn sau 4 lần retry hoặc do địa chỉ email không hợp lệ."
    )
    @GetMapping("/dlq")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCT_OWNER')")
    public ResponseEntity<ApiResponse<PageResponse<EmailLogResponse>>> getDeadLetterQueue(
            @RequestParam(required = false) Map<String, Object> params,
            @RequestParam(name = "sortBy", required = false) List<String> sortByParams
    ) {
        PageResponse<EmailLogResponse> dlq = emailService.getDeadLetterQueue(params, sortByParams);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy danh sách Dead Letter Queue thành công", dlq));
    }

    @Operation(
            summary = "Đếm số lượng email trong Dead Letter Queue",
            description = "API thống kê số lượng email đang nằm trong DLQ để hiển thị badge cảnh báo trên giao diện Admin."
    )
    @GetMapping("/dlq/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCT_OWNER')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countDeadLetterEmails() {
        long count = emailService.countDeadLetterEmails();
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy số lượng email trong DLQ thành công", Map.of("count", count)));
    }

    @Operation(
            summary = "Lấy chi tiết một email log theo ID",
            description = "API xem chi tiết thông tin email, lý do lỗi và số lần đã retry."
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCT_OWNER')")
    public ResponseEntity<ApiResponse<EmailLogResponse>> getEmailLogById(@PathVariable Long id) {
        EmailLogResponse logResponse = emailService.getEmailLogById(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy chi tiết nhật ký email thành công", logResponse));
    }

    @Operation(
            summary = "Thử lại (Retry) email trong Dead Letter Queue",
            description = "API cho phép Admin kích hoạt retry lại email đã bị đưa vào Dead Letter Queue."
    )
    @PostMapping("/{id}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmailLogResponse>> retryEmail(@PathVariable Long id) {
        EmailLogResponse response = emailService.retryEmail(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Đã kích hoạt gửi lại email thành công", response));
    }

    @Operation(
            summary = "Đổi email người nhận và gửi lại (Resend)",
            description = "API cho phép Admin cập nhật địa chỉ email người nhận mới (trường hợp email cũ bị sai) và kích hoạt gửi lại ngay."
    )
    @PostMapping("/{id}/resend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmailLogResponse>> resendEmail(
            @PathVariable Long id,
            @Valid @RequestBody EmailResendRequest request
    ) {
        EmailLogResponse response = emailService.resendEmailWithNewRecipient(
                id,
                request.getNewRecipientEmail(),
                request.getNotes()
        );
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Đã cập nhật email người nhận và kích hoạt gửi lại thành công", response));
    }
}
