package com.example.ecommerce.mail.controller;

import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.mail.dto.response.EmailOutboxResponse;
import com.example.ecommerce.mail.enums.EmailOutboxStatus;
import com.example.ecommerce.mail.service.EmailOutboxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/emails")
@RequiredArgsConstructor
@Tag(
        name = "Email Outbox Administration",
        description = "APIs quản lý hàng đợi gửi email qua Transactional Outbox (danh sách lỗi, gửi lại)."
)
public class EmailAdminController {

    private final EmailOutboxService emailOutboxService;

    /**
     * Quy tắc 9: GET /api/admin/emails?status=FAILED&page=0&size=20 trả danh sách mail lỗi.
     */
    @Operation(summary = "Lấy danh sách email outbox theo trạng thái")
    @GetMapping
    public ResponseEntity<PageResponse<EmailOutboxResponse>> getEmails(
            @RequestParam(name = "status", required = false, defaultValue = "FAILED") String status,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size
    ) {
        EmailOutboxStatus outboxStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                outboxStatus = EmailOutboxStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Nếu status không hợp lệ thì để null
            }
        }

        Page<EmailOutboxResponse> pageResult = emailOutboxService.getEmailOutboxList(outboxStatus, page, size);

        PageResponse<EmailOutboxResponse> response = PageResponse.<EmailOutboxResponse>builder()
                .content(pageResult.getContent())
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .isLast(pageResult.isLast())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Quy tắc 10: POST /api/admin/emails/{id}/resend đặt STATUS = PENDING, ATTEMPTS = 0, NEXT_ATTEMPT_AT = hiện tại.
     * Nếu mail không ở FAILED, hệ thống trả 409 EMAIL_NOT_RESENDABLE.
     */
    @Operation(summary = "Gửi lại email thất bại (đặt lại STATUS = PENDING)")
    @PostMapping("/{id}/resend")
    public ResponseEntity<EmailOutboxResponse> resendEmail(@PathVariable("id") Long id) {
        EmailOutboxResponse result = emailOutboxService.resendFailedEmail(id);
        return ResponseEntity.ok(result);
    }
}
