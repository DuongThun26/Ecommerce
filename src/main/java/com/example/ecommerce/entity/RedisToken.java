package com.example.ecommerce.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.util.concurrent.TimeUnit;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@RedisHash("RedisHash")

public class RedisToken {
    @Id
    private String jwtId;

    @TimeToLive(unit = TimeUnit.DAYS)
    private Long expiredTime;
}
