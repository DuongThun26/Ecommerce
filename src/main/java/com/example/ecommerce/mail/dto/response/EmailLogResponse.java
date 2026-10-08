package com.example.ecommerce.mail.dto.response;

import com.example.ecommerce.mail.enums.EmailStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailLogResponse {
    private Long id;
    private String recipientEmail;
    private String subject;
    private String contentHtml;
    private String attachmentFileName;
    private String attachmentContentType;
    private EmailStatus status;
    private int attemptCount;
    private int maxAttempts;
    private LocalDateTime nextRetryAt;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime sentAt;
    private String lastErrorMessage;
    private String errorType;
    private String metadataInfo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
