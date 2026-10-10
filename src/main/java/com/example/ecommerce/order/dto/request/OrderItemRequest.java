package com.example.ecommerce.order.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemRequest {

    @Schema(
            description = "productId",
            example = "1"
    )
    @NotNull(message = "Vui lòng chọn sản phẩm.")
    @Positive(message = "Vui lòng chọn sản phẩm hợp lệ.")
    private Long productId;

    @Schema(description = "quantity",
            example = "2"
    )
    @NotNull(message = "Nhập số lượng sản phẩm.")
    @Min(value = 1, message = "Số lượng sản phẩm phải lớn hơn hoặc bằng 1.")
    @Max(value = 99, message = "Số lượng sản phẩm phải nhỏ hơn hoặc bằng 99.")
    private Integer quantity;
}
