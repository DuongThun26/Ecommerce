package com.example.ecommerce.payment.dto.response;

import com.example.ecommerce.payment.enums.PaymentStatus;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentStatusResponse {
    private PaymentStatus status;
}
