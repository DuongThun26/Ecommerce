package com.example.ecommerce.auth.service;

import com.example.ecommerce.auth.dto.request.UserRequest;
import com.example.ecommerce.auth.dto.response.UserResponse;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.auth.entity.UserEntity;
import com.example.ecommerce.customer.enums.CustomerStatus;
import com.example.ecommerce.auth.enums.UserRole;
import com.example.ecommerce.auth.enums.UserStatus;
import com.example.ecommerce.common.exception.DuplicateResourceException;
import com.example.ecommerce.customer.repository.CustomerRepository;
import com.example.ecommerce.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public UserResponse create(UserRequest request) {
        if(userRepository.existsByUsername(request.getUsername())){
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }
        if (customerRepository.existsByEmail(request.getEmail()) || userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        UserEntity user = UserEntity.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(encodedPassword)
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

    @Transactional(readOnly = true)
    public List<UserResponse> getAll(){
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new com.example.ecommerce.common.exception.ResourceNotFoundException("User not found with id: " + id));
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateRole(Long id, UserRole role) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new com.example.ecommerce.common.exception.ResourceNotFoundException("User not found with id: " + id));
        user.setRole(role);
        userRepository.save(user);
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateStatus(Long id, UserStatus status) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new com.example.ecommerce.common.exception.ResourceNotFoundException("User not found with id: " + id));
        user.setStatus(status);
        userRepository.save(user);
        return mapToResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new com.example.ecommerce.common.exception.ResourceNotFoundException("User not found with id: " + id));
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

    private UserResponse mapToResponse(UserEntity user) {
        Long customerId = customerRepository.findByUserUsername(user.getUsername())
                .map(CustomerEntity::getId)
                .orElse(null);

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .customerId(customerId)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
