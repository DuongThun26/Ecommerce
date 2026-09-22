package com.example.ecommerce.vnpay.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class VnPayPaymentRequest {
    @NotNull
    private Long orderId;
}
