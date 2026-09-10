package com.example.ecommerce.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JWTInfor {
    private String id;
    private Date issueTime;
    private Date expirationTime;
}
