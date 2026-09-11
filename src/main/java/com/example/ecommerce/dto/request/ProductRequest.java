package com.example.ecommerce.dto.request;

import com.example.ecommerce.enums.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ProductRequest {

    @Schema(description = "name", example = "iPhone 15")
    @NotBlank(message = "Name không được để trống.")
    @Size(max = 255, message = "Name không được vượt quá 255 ký tự.")
    private String name;

    @Schema(description = "price", example = "19990000")
    @NotNull(message = "Price không được để trống.")
    @PositiveOrZero(message = "Price không được nhỏ hơn 0.")
    private BigDecimal price;

    @Schema(description = "stock", example = "10")
    @NotNull(message = "Stock không được để trống.")
    @PositiveOrZero(message = "Stock không được nhỏ hơn 0.")
    private Integer stock;

    private ProductStatus status;
}
