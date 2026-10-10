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
public class AddOrderItemRequest {

    @Schema(description = "ID của sản phẩm", example = "10")
    @NotNull(message = "Sản phẩm không được để trống")
    private Long productId;

    @Schema(description = "Số lượng sản phẩm cần thêm (1-99)", example = "3")
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng sản phẩm phải lớn hơn hoặc bằng 1")
    @Max(value = 99, message = "Số lượng sản phẩm phải nhỏ hơn hoặc bằng 99")
    private Integer quantity;
}
