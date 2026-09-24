package com.example.ecommerce.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpdateRequest {
    @NotEmpty(message = "Danh sách sản phẩm không được để trống")
    private List<@Valid OrderItemRequest> orderItems;
}
