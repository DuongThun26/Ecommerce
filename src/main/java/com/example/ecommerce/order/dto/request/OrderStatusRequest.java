package com.example.ecommerce.order.dto.request;

import com.example.ecommerce.order.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderStatusRequest {

    @Schema(description = "status", example = "PROCESSING")
    @NotNull(message = "Vui lòng chọn trạng thái đơn hàng.")
    private OrderStatus status;

    @Schema(description = "reason", example = "Đã xác nhận và đang xử lý")
    private String reason;
}
