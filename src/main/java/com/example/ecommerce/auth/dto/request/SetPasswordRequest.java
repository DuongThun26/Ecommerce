package com.example.ecommerce.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SetPasswordRequest {

    @Schema(description = "Token kích hoạt từ email", example = "a9b1c2d3-...")
    @NotBlank(message = "Token kích hoạt không được để trống.")
    private String token;

    @Schema(description = "Mật khẩu mới do người dùng đặt", example = "Password@123")
    @NotBlank(message = "Mật khẩu không được để trống.")
    @Size(min = 6, max = 50, message = "Mật khẩu phải từ 6 đến 50 ký tự.")
    private String newPassword;
}
