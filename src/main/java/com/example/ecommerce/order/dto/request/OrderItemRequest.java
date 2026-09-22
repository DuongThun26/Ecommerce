package com.example.ecommerce.order.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
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
    @Positive(message = "Số lượng sản phẩm phải lớn hơn 0.")
    private Integer quantity;
}
