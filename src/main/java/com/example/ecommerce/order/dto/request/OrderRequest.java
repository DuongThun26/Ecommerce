package com.example.ecommerce.order.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class OrderRequest {

    @Schema(
            description = "customerId",
            example = "1"
    )
    @NotNull
    private Long customerId;

    @Schema(description = "orderItems",
        example = "[" +
                    "{" +
                        "\"productId\": 1, " +
                        "\"quantity\": 2" +
                    "}, " +
                    "{\"productId\": 2, " +
                        "\"quantity\": 3" +
                    "}" +
                "]"
    )
    @NotEmpty(message = "Vui lòng thêm ít nhất một sản phẩm vào đơn hàng.")
    @Valid
    private List<OrderItemRequest> orderItems;
}
