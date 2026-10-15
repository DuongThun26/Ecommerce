package com.example.ecommerce.auth.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActivationVerifyResponse {
    private boolean valid;
    private String message;
    private String username;
    private String email;
    private String name;
}
