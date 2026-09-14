package com.example.ecommerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {
    @Schema(description = "username", example = "thuandv20")
    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 50, message = "Username must be between 4 and 50 characters")
    private String username;

    @Schema(description = "password", example = "Thuan@2004")
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters")
    private String password;

    @Schema(description = "name", example = "Nguyen Van A")
    @NotBlank(message = "Name không được để trống.")
    @Size(max = 100, message = "Name không được vượt quá 100 ký tự.")
    private String name;

    @Schema(description = "email", example = "thuandv20@fpt.com")
    @NotBlank(message = "Email không được để trống.")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự.")
    @Email(message = "Email không hợp lệ.")
    private String email;

    @Schema(description = "phone", example = "0987654321")
    @Size(max = 15, message = "Phone không được vượt quá 15 ký tự.")
    private String phone;
}
