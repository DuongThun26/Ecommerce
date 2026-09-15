package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.LoginRequest;
import com.example.ecommerce.dto.request.RefreshTokenRequest;
import com.example.ecommerce.dto.request.RegisterRequest;
import com.example.ecommerce.dto.response.JWTInfor;
import com.example.ecommerce.dto.response.LoginResponse;
import com.example.ecommerce.dto.response.TokenPayload;
import com.example.ecommerce.entity.CustomerEntity;
import com.example.ecommerce.entity.UserEntity;
import com.example.ecommerce.exception.DuplicateResourceException;
import com.example.ecommerce.repository.AccessTokenBlackListRepository;
import com.example.ecommerce.repository.CustomerRepository;
import com.example.ecommerce.repository.RefreshTokenWhiteListRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.testdata.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JWTService jwtService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private RefreshTokenWhiteListRepository refreshTokenWhiteListRepository;
    @Mock
    private AccessTokenBlackListRepository accessTokenBlackListRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    @DisplayName("register should create user and customer")
    void registerShouldCreateUserAndCustomer() {
        RegisterRequest request = TestDataFactory.createRegisterRequest("tester", "tester@example.com", "password123");
        when(userRepository.existsByUsername("tester")).thenReturn(false);
        when(customerRepository.existsByEmail("tester@example.com")).thenReturn(false);
        when(userRepository.existsByEmail("tester@example.com")).thenReturn(false);

        UserEntity savedUser = new UserEntity();
        savedUser.setId(1L);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        CustomerEntity savedCustomer = new CustomerEntity();
        savedCustomer.setId(2L);
        when(customerRepository.save(any(CustomerEntity.class))).thenReturn(savedCustomer);

        var response = authenticationService.register(request);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("register should reject duplicate username")
    void registerShouldRejectDuplicateUsername() {
        RegisterRequest request = TestDataFactory.createRegisterRequest("tester", "tester@example.com", "password123");
        when(userRepository.existsByUsername("tester")).thenReturn(true);
        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("login should issue access and refresh token")
    void loginShouldIssueTokens() {
        LoginRequest request = TestDataFactory.createLoginRequest("tester", "password123");
        UserEntity user = new UserEntity();
        user.setUsername("tester");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

        TokenPayload refresh = new TokenPayload();
        refresh.setJwtId("rid");
        refresh.setToken("refresh");
        refresh.setTimeToLive(1000L);
        TokenPayload access = new TokenPayload();
        access.setToken("access");

        when(jwtService.generateRefreshToken(user)).thenReturn(refresh);
        when(jwtService.generateAccessToken(user, "rid")).thenReturn(access);

        LoginResponse response = authenticationService.login(request);
        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
    }

    @Test
    @DisplayName("refresh should reject invalid refresh token")
    void refreshShouldRejectInvalidToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("bad");
        when(jwtService.verifyRefreshToken("bad")).thenReturn(false);
        assertThatThrownBy(() -> authenticationService.refresh(request)).hasMessageContaining("Invalid refresh token");
    }

    @Test
    @DisplayName("refresh should rotate refresh token")
    void refreshShouldRotateToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("old");

        JWTInfor info = new JWTInfor();
        info.setId("old-id");
        info.setSubject("tester");

        UserEntity user = new UserEntity();
        user.setUsername("tester");

        TokenPayload refresh = new TokenPayload();
        refresh.setJwtId("new-id");
        refresh.setToken("new-refresh");
        refresh.setTimeToLive(1000L);

        TokenPayload access = new TokenPayload();
        access.setToken("new-access");

        when(jwtService.verifyRefreshToken("old")).thenReturn(true);
        when(jwtService.parse("old")).thenReturn(info);
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(user));
        when(jwtService.generateRefreshToken(user)).thenReturn(refresh);
        when(jwtService.generateAccessToken(user, "new-id")).thenReturn(access);

        LoginResponse response = authenticationService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        verify(refreshTokenWhiteListRepository).deleteById("old-id");
    }
}
