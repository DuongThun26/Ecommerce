package com.example.ecommerce.customer.dto.request;

import com.example.ecommerce.customer.enums.CustomerStatus;
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
public class CustomerRequest {

    @Schema(
            description = "name",
            example = "thuandv20"
    )
    @NotBlank(message = "Name không được để trống.")
    @Size(max = 100, message = "Name không được vượt quá 100 ký tự.")
    private String name;

    @Schema(
            description = "email",
            example = "thuandv20@fpt.com"
    )
    @NotBlank(message = "Email không được để trống.")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự.")
    @Email(message = "Email không hợp lệ.")
    private String email;

    @Schema(
            description = "phone",
            example = "0987654321"
    )
    @Size(max = 15, message = "Phone không được vượt quá 15 ký tự.")
    private String phone;

    private CustomerStatus status;
}
