package com.example.ecommerce.mail.dto.request;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailResendRequest {

    @Email(message = "Địa chỉ email người nhận không đúng định dạng.")
    private String newRecipientEmail;

    private String notes;
}
