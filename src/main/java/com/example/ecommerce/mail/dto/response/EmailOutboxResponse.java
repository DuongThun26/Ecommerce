package com.example.ecommerce.mail.dto.response;

import com.example.ecommerce.mail.enums.EmailOutboxStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailOutboxResponse {
    private Long id;
    private String template;
    private Long referenceId;
    private String recipient;
    private EmailOutboxStatus status;
    private Integer attempts;
    private LocalDateTime nextAttemptAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;

    public static EmailOutboxResponse fromEntity(com.example.ecommerce.mail.entity.EmailOutboxEntity entity) {
        if (entity == null) return null;
        return EmailOutboxResponse.builder()
                .id(entity.getId())
                .template(entity.getTemplate())
                .referenceId(entity.getReferenceId())
                .recipient(entity.getRecipient())
                .status(entity.getStatus())
                .attempts(entity.getAttempts())
                .nextAttemptAt(entity.getNextAttemptAt())
                .lastError(entity.getLastError())
                .createdAt(entity.getCreatedAt())
                .sentAt(entity.getSentAt())
                .build();
    }
}
