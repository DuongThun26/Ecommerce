package com.example.ecommerce.payment.vnpay.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class VnPayPaymentResponse {
    private Long paymentId;
    private String transactionRef;
    private String paymentUrl;
}
