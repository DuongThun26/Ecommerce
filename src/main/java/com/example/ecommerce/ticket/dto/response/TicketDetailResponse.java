package com.example.ecommerce.ticket.dto.response;

import com.example.ecommerce.order.dto.response.OrderResponse;
import com.example.ecommerce.ticket.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketDetailResponse {
    private Long id;
    private String ticketCode;
    private Long orderId;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private BigDecimal totalAmount;
    private TicketStatus status;
    private String title;
    private String description;
    private Long reviewerId;
    private String reviewerUsername;
    private String reviewNotes;
    private String rejectionReason;
    private String returnedReason;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private OrderResponse orderDetails;
}
