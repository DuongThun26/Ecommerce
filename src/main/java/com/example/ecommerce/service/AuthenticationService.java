package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.LoginRequest;
import com.example.ecommerce.dto.request.RefreshTokenRequest;
import com.example.ecommerce.dto.response.JWTInfor;
import com.example.ecommerce.dto.response.LoginResponse;
import com.example.ecommerce.dto.response.TokenPayload;
import com.example.ecommerce.entity.AccessTokenBlackList;
import com.example.ecommerce.entity.RefreshTokenWhiteList;
import com.example.ecommerce.entity.UserEntity;
import com.example.ecommerce.repository.AccessTokenBlackListRepository;
import com.example.ecommerce.repository.RefreshTokenWhiteListRepository;
import com.example.ecommerce.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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
    private final UserRepository userRepository;
    private final RefreshTokenWhiteListRepository refreshTokenWhiteListRepository;
    private final AccessTokenBlackListRepository accessTokenBlackListRepository;

    public LoginResponse login(LoginRequest request){
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        UserEntity user = (UserEntity) authentication.getPrincipal();
        return issueTokens(user);
    }

    public LoginResponse refresh(RefreshTokenRequest request) throws ParseException, JOSEException {
        String oldRefreshToken = request.getRefreshToken();
        if(oldRefreshToken == null || !jwtService.verifyRefreshToken(oldRefreshToken)){
            throw new BadCredentialsException("Invalid refresh token!");
        }
        JWTInfor jwtInfor = jwtService.parse(oldRefreshToken);
        UserEntity user = userRepository.findByUsername(jwtInfor.getSubject()).orElseThrow(() -> new BadCredentialsException("User not found!"));
        refreshTokenWhiteListRepository.deleteById(jwtInfor.getId());
        return issueTokens(user);
    }

    public LoginResponse issueTokens(UserEntity user){
        TokenPayload refreshTokenPayload = jwtService.generateRefreshToken(user);
        TokenPayload accessTokenPayload = jwtService.generateAccessToken(user, refreshTokenPayload.getJwtId());

        RefreshTokenWhiteList refreshTokenWhiteList = RefreshTokenWhiteList.builder()
                .jwtId(refreshTokenPayload.getJwtId())
                .timeToLive(refreshTokenPayload.getTimeToLive())
                .build();
        refreshTokenWhiteListRepository.save(refreshTokenWhiteList);
        return LoginResponse.builder()
                .accessToken(accessTokenPayload.getToken())
                .refreshToken(refreshTokenPayload.getToken())
                .build();
    }
    public void logout(String token) throws ParseException {
        JWTInfor jwtInfor = jwtService.parse(token);
        String jwtId = jwtInfor.getId();
        Date issueTime = jwtInfor.getIssueTime();
        Date expirationTime = jwtInfor.getExpirationTime();
        if(expirationTime.before(new Date())) return;
        AccessTokenBlackList accessTokenBlackList = AccessTokenBlackList.builder()
                .jwtId(jwtId)
                .timeToLive(expirationTime.getTime() - new Date().getTime())
                .build();
        accessTokenBlackListRepository.save(accessTokenBlackList);
        String refreshJwtId = jwtInfor.getRefreshTokenId();
        if(refreshJwtId != null && refreshTokenWhiteListRepository.existsById(refreshJwtId)){
            refreshTokenWhiteListRepository.deleteById(refreshJwtId);
        }
    }
}
