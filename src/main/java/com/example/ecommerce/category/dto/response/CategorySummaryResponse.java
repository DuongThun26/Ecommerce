package com.example.ecommerce.category.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySummaryResponse {
    private Long id;
    private String name;
    private String code;
    private Long managedByUserId;
    private String managedByUsername;
}
