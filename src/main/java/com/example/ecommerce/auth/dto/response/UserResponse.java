package com.example.ecommerce.auth.dto.response;

import com.example.ecommerce.auth.enums.UserRole;
import com.example.ecommerce.auth.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private UserRole role;
    private UserStatus status;
    private Long customerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
