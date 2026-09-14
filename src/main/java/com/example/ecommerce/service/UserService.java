package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.UserRequest;
import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.entity.CustomerEntity;
import com.example.ecommerce.entity.UserEntity;
import com.example.ecommerce.enums.CustomerStatus;
import com.example.ecommerce.enums.UserRole;
import com.example.ecommerce.enums.UserStatus;
import com.example.ecommerce.exception.DuplicateResourceException;
import com.example.ecommerce.repository.CustomerRepository;
import com.example.ecommerce.repository.UserRepository;
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

    public List<UserResponse> getAll(){
        List<UserResponse> users = userRepository.findAll().stream()
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .customerId(null)
                        .build())
                .toList();
        return users;
    }
}
