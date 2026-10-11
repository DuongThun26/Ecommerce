package com.example.ecommerce.mail.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemMailDto {
    private String productName;
    private Integer quantity;
    private String unitPriceFormatted;
    private String subtotalFormatted;
}
