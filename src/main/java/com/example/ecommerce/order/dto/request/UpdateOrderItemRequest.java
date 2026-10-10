package com.example.ecommerce.order.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderItemRequest {

    @Schema(description = "Số lượng sản phẩm mới (1-99)", example = "2")
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng sản phẩm phải từ 1 đến 99")
    @Max(value = 99, message = "Số lượng sản phẩm phải từ 1 đến 99")
    private Integer quantity;
}
