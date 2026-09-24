package com.example.ecommerce.order.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderCancelRequest {

    @Schema(description = "reason", example = "Khách hàng yêu cầu hủy trước khi thanh toán")
    private String reason;
}
