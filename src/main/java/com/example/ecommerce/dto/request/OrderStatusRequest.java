package com.example.ecommerce.dto.request;

import com.example.ecommerce.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderStatusRequest {

    // Trạng thái mới mà client muốn cập nhật cho đơn hàng.
    @Schema(description = "status", example = "CONFIRMED")
    @NotNull(message = "Vui lòng chọn trạng thái đơn hàng.")
    private OrderStatus status;
}
