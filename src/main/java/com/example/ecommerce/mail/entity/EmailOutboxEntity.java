package com.example.ecommerce.mail.entity;

import com.example.ecommerce.mail.enums.EmailOutboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "email_outbox",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_email_outbox_tpl_ref", columnNames = {"template", "reference_id"})
        },
        indexes = {
                @Index(name = "idx_email_outbox_status_next", columnList = "status, next_attempt_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailOutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template", nullable = false, length = 50)
    private String template;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(name = "recipient", nullable = false, length = 255)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmailOutboxStatus status;

    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}
