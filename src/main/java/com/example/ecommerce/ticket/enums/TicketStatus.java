package com.example.ecommerce.ticket.enums;

import lombok.Getter;

@Getter
public enum TicketStatus {
    PENDING_APPROVAL("Phiếu chờ Product Owner phê duyệt"),
    APPROVED("Phiếu đã được duyệt - chuyển sang chờ thanh toán"),
    REJECTED("Phiếu bị từ chối"),
    RETURNED("Phiếu bị trả lại yêu cầu khách hàng chỉnh sửa"),
    CANCELLED("Phiếu đã bị hủy");

    private final String description;

    TicketStatus(String description) {
        this.description = description;
    }

    public boolean isTerminal() {
        return this == REJECTED || this == CANCELLED;
    }

    public boolean canBeReviewed() {
        return this == PENDING_APPROVAL;
    }
}
