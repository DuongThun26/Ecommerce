package com.example.ecommerce.mail.enums;

import lombok.Getter;

@Getter
public enum EmailStatus {
    PENDING("Đang chờ gửi"),
    PROCESSING("Đang xử lý gửi"),
    SENT("Đã gửi thành công"),
    RETRYING("Đang trong hàng đợi gửi lại"),
    DEAD_LETTER("Chuyển vào Dead Letter Queue (thất bại hoàn toàn)");

    private final String description;

    EmailStatus(String description) {
        this.description = description;
    }

    public boolean isTerminal() {
        return this == SENT || this == DEAD_LETTER;
    }
}
