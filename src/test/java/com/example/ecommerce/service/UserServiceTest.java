package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.UserRequest;
import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.entity.CustomerEntity;
import com.example.ecommerce.entity.UserEntity;
import com.example.ecommerce.exception.DuplicateResourceException;
import com.example.ecommerce.repository.CustomerRepository;
import com.example.ecommerce.repository.UserRepository;
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
}
