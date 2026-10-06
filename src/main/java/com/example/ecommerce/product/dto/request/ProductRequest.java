package com.example.ecommerce.product.dto.request;

import com.example.ecommerce.product.enums.ProductStatus;
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

    @Schema(description = "ID của loại hàng hóa (Category)", example = "1")
    private Long categoryId;

    @Schema(description = "Danh sách URL ảnh của sản phẩm trên Cloudinary", example = "[\"https://res.cloudinary.com/.../image1.jpg\"]")
    private java.util.List<String> imageUrls;
}
