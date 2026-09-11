package com.example.ecommerce.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerResponseSummary {
    private Long id;
    private String name;
}
