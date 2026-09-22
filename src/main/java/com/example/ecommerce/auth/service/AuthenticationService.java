package com.example.ecommerce.auth.service;

import com.example.ecommerce.auth.dto.request.LoginRequest;
import com.example.ecommerce.auth.dto.request.RegisterRequest;
import com.example.ecommerce.auth.dto.request.RefreshTokenRequest;
import com.example.ecommerce.auth.dto.response.JWTInfor;
import com.example.ecommerce.auth.dto.response.LoginResponse;
import com.example.ecommerce.auth.dto.response.TokenPayload;
import com.example.ecommerce.auth.dto.response.UserResponse;
import com.example.ecommerce.auth.entity.AccessTokenBlackList;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.auth.entity.RefreshTokenWhiteList;
import com.example.ecommerce.auth.entity.UserEntity;
import com.example.ecommerce.customer.enums.CustomerStatus;
import com.example.ecommerce.auth.enums.UserRole;
import com.example.ecommerce.auth.enums.UserStatus;
import com.example.ecommerce.common.exception.DuplicateResourceException;
import com.example.ecommerce.auth.repository.AccessTokenBlackListRepository;
import com.example.ecommerce.customer.repository.CustomerRepository;
import com.example.ecommerce.auth.repository.RefreshTokenWhiteListRepository;
import com.example.ecommerce.auth.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RefreshTokenWhiteListRepository refreshTokenWhiteListRepository;
    private final AccessTokenBlackListRepository accessTokenBlackListRepository;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }
        if (customerRepository.existsByEmail(request.getEmail()) || userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        UserEntity user = UserEntity.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        CustomerEntity customer = CustomerEntity.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .status(CustomerStatus.ACTIVE)
                .deleted(false)
                .user(user)
                .build();
        customerRepository.save(customer);

        return UserResponse.builder()
                .id(user.getId())
                .customerId(customer.getId())
                .build();
    }

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
