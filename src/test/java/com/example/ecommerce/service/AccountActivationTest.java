package com.example.ecommerce.service;

import com.example.ecommerce.auth.dto.request.SetPasswordRequest;
import com.example.ecommerce.auth.dto.response.ActivationVerifyResponse;
import com.example.ecommerce.auth.entity.AccountActivationTokenEntity;
import com.example.ecommerce.auth.entity.UserEntity;
import com.example.ecommerce.auth.enums.UserRole;
import com.example.ecommerce.auth.enums.UserStatus;
import com.example.ecommerce.auth.repository.AccountActivationTokenRepository;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.auth.service.AuthenticationService;
import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.customer.enums.CustomerStatus;
import com.example.ecommerce.customer.repository.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountActivationTest {

    @Mock
    private AccountActivationTokenRepository accountActivationTokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    @DisplayName("verifyActivationToken should return valid=true for unexpired, unused token")
    void verifyActivationTokenShouldReturnTrueForValidToken() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("nguyenana1234")
                .email("ana@example.com")
                .status(UserStatus.PENDING_ACTIVATION)
                .build();

        AccountActivationTokenEntity tokenEntity = AccountActivationTokenEntity.builder()
                .id(10L)
                .token("valid-token-uuid")
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .build();

        when(accountActivationTokenRepository.findByToken("valid-token-uuid")).thenReturn(Optional.of(tokenEntity));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(
                CustomerEntity.builder().id(5L).name("Nguyễn An").user(user).build()
        ));

        ActivationVerifyResponse response = authenticationService.verifyActivationToken("valid-token-uuid");

        assertThat(response.isValid()).isTrue();
        assertThat(response.getUsername()).isEqualTo("nguyenana1234");
        assertThat(response.getEmail()).isEqualTo("ana@example.com");
        assertThat(response.getName()).isEqualTo("Nguyễn An");
    }

    @Test
    @DisplayName("verifyActivationToken should return valid=false for expired token")
    void verifyActivationTokenShouldReturnFalseForExpiredToken() {
        UserEntity user = UserEntity.builder().id(1L).username("testuser").build();

        AccountActivationTokenEntity tokenEntity = AccountActivationTokenEntity.builder()
                .id(10L)
                .token("expired-token")
                .user(user)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        when(accountActivationTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(tokenEntity));

        ActivationVerifyResponse response = authenticationService.verifyActivationToken("expired-token");

        assertThat(response.isValid()).isFalse();
        assertThat(response.getMessage()).contains("hết hạn");
    }

    @Test
    @DisplayName("verifyActivationToken should return valid=false for used token")
    void verifyActivationTokenShouldReturnFalseForUsedToken() {
        UserEntity user = UserEntity.builder().id(1L).username("testuser").build();

        AccountActivationTokenEntity tokenEntity = AccountActivationTokenEntity.builder()
                .id(10L)
                .token("used-token")
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(10))
                .usedAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(accountActivationTokenRepository.findByToken("used-token")).thenReturn(Optional.of(tokenEntity));

        ActivationVerifyResponse response = authenticationService.verifyActivationToken("used-token");

        assertThat(response.isValid()).isFalse();
        assertThat(response.getMessage()).contains("đã được sử dụng");
    }

    @Test
    @DisplayName("setPasswordAndActivate should activate user and customer, invalidate token")
    void setPasswordAndActivateShouldActivateAccount() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("nguyenana1234")
                .email("ana@example.com")
                .status(UserStatus.PENDING_ACTIVATION)
                .build();

        AccountActivationTokenEntity tokenEntity = AccountActivationTokenEntity.builder()
                .id(10L)
                .token("valid-token")
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .build();

        CustomerEntity customer = CustomerEntity.builder()
                .id(5L)
                .name("Nguyễn An")
                .status(CustomerStatus.INACTIVE)
                .user(user)
                .build();

        when(accountActivationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(tokenEntity));
        when(passwordEncoder.encode("NewSecretPassword@123")).thenReturn("encodedSecretHash");
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(customer));

        SetPasswordRequest request = new SetPasswordRequest("valid-token", "NewSecretPassword@123");
        authenticationService.setPasswordAndActivate(request);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getPassword()).isEqualTo("encodedSecretHash");
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(tokenEntity.getUsedAt()).isNotNull();

        verify(userRepository).save(user);
        verify(customerRepository).save(customer);
        verify(accountActivationTokenRepository).save(tokenEntity);
    }

    @Test
    @DisplayName("setPasswordAndActivate should throw BadRequestException if token already used")
    void setPasswordAndActivateShouldRejectUsedToken() {
        UserEntity user = UserEntity.builder().id(1L).username("testuser").build();

        AccountActivationTokenEntity tokenEntity = AccountActivationTokenEntity.builder()
                .id(10L)
                .token("already-used-token")
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(10))
                .usedAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(accountActivationTokenRepository.findByToken("already-used-token")).thenReturn(Optional.of(tokenEntity));

        SetPasswordRequest request = new SetPasswordRequest("already-used-token", "NewSecretPassword@123");

        assertThatThrownBy(() -> authenticationService.setPasswordAndActivate(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("đã được sử dụng");
    }
}
