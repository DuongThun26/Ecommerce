package com.example.ecommerce.category.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @Schema(description = "Tên loại hàng", example = "Điện thoại & Phụ kiện")
    @NotBlank(message = "Tên loại hàng không được để trống.")
    @Size(max = 255, message = "Tên loại hàng không được vượt quá 255 ký tự.")
    private String name;

    @Schema(description = "Mã loại hàng duy nhất (viết hoa)", example = "SMARTPHONE")
    @NotBlank(message = "Mã loại hàng không được để trống.")
    @Size(max = 100, message = "Mã loại hàng không được vượt quá 100 ký tự.")
    private String code;

    @Schema(description = "Mô tả chi tiết loại hàng", example = "Các dòng điện thoại thông minh và linh kiện")
    private String description;

    @Schema(description = "ID của Product Owner được chỉ định quản lý loại hàng này", example = "2")
    private Long managedByUserId;
}
