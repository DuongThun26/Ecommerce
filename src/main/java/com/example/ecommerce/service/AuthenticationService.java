package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.LoginRequest;
import com.example.ecommerce.dto.response.JWTInfor;
import com.example.ecommerce.dto.response.LoginResponse;
import com.example.ecommerce.entity.RedisToken;
import com.example.ecommerce.entity.UserEntity;
import com.example.ecommerce.repository.RedisTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final RedisTokenRepository redisTokenRepository;

    public LoginResponse login(LoginRequest request){
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        UserEntity user = (UserEntity) authentication.getPrincipal();
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
    public void logout(String token) throws ParseException {
        JWTInfor jwtInfor = jwtService.parse(token);
        String jwtId = jwtInfor.getId();
        Date issueTime = jwtInfor.getIssueTime();
        Date expirationTime = jwtInfor.getExpirationTime();
        if(expirationTime.before(new Date())) return;
        RedisToken redisToken = RedisToken.builder()
                .jwtId(jwtId)
                .expiredTime(expirationTime.getTime() - issueTime.getTime())
                .build();
        redisTokenRepository.save(redisToken);
    }
}
