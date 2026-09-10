package com.example.ecommerce.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

@Component
@RequiredArgsConstructor
public class JWTDecoderConfiguration implements JwtDecoder {

    @Override
    public Jwt decode(String token) throws JwtException {
        return null;
    }
}
