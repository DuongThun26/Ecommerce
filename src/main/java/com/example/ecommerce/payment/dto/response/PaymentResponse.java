package com.example.ecommerce.payment.dto.response;

import com.example.ecommerce.order.entity.OrderEntity;
import com.example.ecommerce.payment.enums.PaymentMethod;
import com.example.ecommerce.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String transactionRef;
    private String transactionNo;
    private String bankCode;
}
