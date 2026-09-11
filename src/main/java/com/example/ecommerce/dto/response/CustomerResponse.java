package com.example.ecommerce.dto.response;

import com.example.ecommerce.enums.CustomerStatus;
import lombok.*;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private CustomerStatus status;
}
