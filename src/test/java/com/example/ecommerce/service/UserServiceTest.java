package com.example.ecommerce.service;

import com.example.ecommerce.auth.service.UserService;
import com.example.ecommerce.auth.dto.request.UserRequest;
import com.example.ecommerce.auth.dto.response.UserResponse;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.auth.entity.UserEntity;
import com.example.ecommerce.common.exception.DuplicateResourceException;
import com.example.ecommerce.customer.repository.CustomerRepository;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.testdata.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private com.example.ecommerce.category.repository.CategoryRepository categoryRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("create should persist user and customer")
    void createShouldPersistUserAndCustomer() {
        UserRequest request = TestDataFactory.createUserRequest("newuser", "new@example.com", "password123");
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(customerRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

        UserEntity savedUser = new UserEntity();
        savedUser.setId(10L);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        CustomerEntity savedCustomer = new CustomerEntity();
        savedCustomer.setId(20L);
        when(customerRepository.save(any(CustomerEntity.class))).thenReturn(savedCustomer);

        UserResponse response = userService.create(request);

        assertThat(response).isNotNull();
        verify(userRepository).save(any(UserEntity.class));
        verify(customerRepository).save(any(CustomerEntity.class));
    }

    @Test
    @DisplayName("create should reject duplicate username")
    void createShouldRejectDuplicateUsername() {
        UserRequest request = TestDataFactory.createUserRequest("existing", "existing@example.com", "password123");
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    @DisplayName("getAll should map list size correctly")
    void getAllShouldMapList() {
        UserEntity u1 = new UserEntity();
        u1.setId(1L);
        UserEntity u2 = new UserEntity();
        u2.setId(2L);
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<UserResponse> result = userService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(UserResponse::getId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("getProfile should return profile details and customer information")
    void getProfileShouldReturnProfileDetails() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .role(com.example.ecommerce.auth.enums.UserRole.CUSTOMER)
                .status(com.example.ecommerce.auth.enums.UserStatus.ACTIVE)
                .build();

        CustomerEntity customer = CustomerEntity.builder()
                .id(10L)
                .name("John Doe")
                .email("john@example.com")
                .phone("0987654321")
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(java.util.Optional.of(user));
        when(customerRepository.findByUserUsername("john_doe")).thenReturn(java.util.Optional.of(customer));

        com.example.ecommerce.auth.dto.response.UserProfileResponse profile = userService.getProfile("john_doe");

        assertThat(profile).isNotNull();
        assertThat(profile.getUsername()).isEqualTo("john_doe");
        assertThat(profile.getName()).isEqualTo("John Doe");
        assertThat(profile.getEmail()).isEqualTo("john@example.com");
        assertThat(profile.getPhone()).isEqualTo("0987654321");
    }

    @Test
    @DisplayName("updateProfile should update user and customer info")
    void updateProfileShouldUpdateInfo() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("john_doe")
                .email("old@example.com")
                .role(com.example.ecommerce.auth.enums.UserRole.CUSTOMER)
                .status(com.example.ecommerce.auth.enums.UserStatus.ACTIVE)
                .build();

        CustomerEntity customer = CustomerEntity.builder()
                .id(10L)
                .name("Old Name")
                .email("old@example.com")
                .phone("0123456789")
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(java.util.Optional.of(user));
        when(customerRepository.findByUserUsername("john_doe")).thenReturn(java.util.Optional.of(customer));
        when(userRepository.existsByEmailAndIdNot("new@example.com", 1L)).thenReturn(false);
        when(customerRepository.existsByEmailAndIdNot("new@example.com", 10L)).thenReturn(false);

        com.example.ecommerce.auth.dto.request.UpdateProfileRequest request = com.example.ecommerce.auth.dto.request.UpdateProfileRequest.builder()
                .name("New Name")
                .email("new@example.com")
                .phone("0987654321")
                .build();

        com.example.ecommerce.auth.dto.response.UserProfileResponse updated = userService.updateProfile("john_doe", request);

        assertThat(updated).isNotNull();
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(customer.getName()).isEqualTo("New Name");
        assertThat(customer.getPhone()).isEqualTo("0987654321");
    }

    @Test
    @DisplayName("changePassword should encode and save new password")
    void changePasswordShouldEncodeAndSave() {
        org.springframework.security.crypto.password.PasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("john_doe")
                .password(encoder.encode("OldPassword123"))
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(java.util.Optional.of(user));

        com.example.ecommerce.auth.dto.request.ChangePasswordRequest request = com.example.ecommerce.auth.dto.request.ChangePasswordRequest.builder()
                .currentPassword("OldPassword123")
                .newPassword("NewPassword456")
                .confirmPassword("NewPassword456")
                .build();

        userService.changePassword("john_doe", request);

        assertThat(encoder.matches("NewPassword456", user.getPassword())).isTrue();
        verify(userRepository).save(user);
    }
}
