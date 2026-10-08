package com.example.ecommerce.auth.dto.response;

import com.example.ecommerce.auth.enums.UserRole;
import com.example.ecommerce.auth.enums.UserStatus;
import com.example.ecommerce.category.dto.response.CategorySummaryResponse;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String username;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
    private UserStatus status;
    private Long customerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CategorySummaryResponse> managedCategories;
}
