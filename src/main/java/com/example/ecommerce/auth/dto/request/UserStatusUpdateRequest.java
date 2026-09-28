package com.example.ecommerce.auth.dto.request;

import com.example.ecommerce.auth.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusUpdateRequest {
    @NotNull(message = "Status không được để trống")
    private UserStatus status;
}
