package com.example.ecommerce.customer.dto.response;

import com.example.ecommerce.customer.enums.CustomerStatus;
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
    private String username;
    private String userStatus;
}
