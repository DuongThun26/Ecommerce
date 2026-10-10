package com.example.ecommerce.order.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {

    @Schema(
            description = "customerId (tùy chọn; nếu để trống hệ thống sẽ tự động lấy thông tin khách hàng từ tài khoản đăng nhập)",
            example = "1"
    )
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
    @JsonProperty("orderItems")
    @JsonAlias({"items", "orderItems"})
    @NotEmpty(message = "Vui lòng thêm ít nhất một sản phẩm vào đơn hàng.")
    @Valid
    private List<OrderItemRequest> orderItems;

    public void setItems(List<OrderItemRequest> items) {
        this.orderItems = items;
    }

    public List<OrderItemRequest> getItems() {
        return this.orderItems;
    }
}
