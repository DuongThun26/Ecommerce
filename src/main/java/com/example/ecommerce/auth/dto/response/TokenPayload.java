package com.example.ecommerce.auth.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenPayload {
    private String token;
    private Long timeToLive;
    private String jwtId;
}
