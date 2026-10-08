package com.example.ecommerce.auth.service;

import com.example.ecommerce.auth.dto.request.ChangePasswordRequest;
import com.example.ecommerce.auth.dto.request.UpdateProfileRequest;
import com.example.ecommerce.auth.dto.request.UserRequest;
import com.example.ecommerce.auth.dto.response.UserProfileResponse;
import com.example.ecommerce.auth.dto.response.UserResponse;
import com.example.ecommerce.auth.entity.UserEntity;
import com.example.ecommerce.auth.enums.UserRole;
import com.example.ecommerce.auth.enums.UserStatus;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.category.dto.response.CategorySummaryResponse;
import com.example.ecommerce.category.entity.CategoryEntity;
import com.example.ecommerce.category.repository.CategoryRepository;
import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.common.exception.DuplicateResourceException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.customer.enums.CustomerStatus;
import com.example.ecommerce.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public UserResponse create(UserRequest request) {
        if(userRepository.existsByUsername(request.getUsername())){
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }
        if (customerRepository.existsByEmail(request.getEmail()) || userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }
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
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new BadRequestException("Không tìm thấy thông tin xác thực của người dùng");
        }
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người dùng: " + username));

        Optional<CustomerEntity> customerOpt = customerRepository.findByUserUsername(username);

        String name = customerOpt.map(CustomerEntity::getName).orElse(user.getUsername());
        String phone = customerOpt.map(CustomerEntity::getPhone).orElse(null);
        Long customerId = customerOpt.map(CustomerEntity::getId).orElse(null);

        List<CategorySummaryResponse> managedCategories = Collections.emptyList();
        if (user.getRole() == UserRole.PRODUCT_OWNER || user.getRole() == UserRole.ADMIN) {
            List<CategoryEntity> categories;
            if (user.getRole() == UserRole.ADMIN) {
                categories = categoryRepository.findAllByDeletedFalse();
            } else {
                categories = categoryRepository.findByManagedByUsernameAndDeletedFalse(username);
            }
            managedCategories = categories.stream()
                    .map(cat -> CategorySummaryResponse.builder()
                            .id(cat.getId())
                            .name(cat.getName())
                            .code(cat.getCode())
                            .managedByUserId(cat.getManagedBy() != null ? cat.getManagedBy().getId() : null)
                            .managedByUsername(cat.getManagedBy() != null ? cat.getManagedBy().getUsername() : null)
                            .build())
                    .toList();
        }

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(name)
                .email(user.getEmail())
                .phone(phone)
                .role(user.getRole())
                .status(user.getStatus())
                .customerId(customerId)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .managedCategories(managedCategories)
                .build();
    }

    @Transactional
    public UserProfileResponse updateProfile(String username, UpdateProfileRequest request) {
        if (username == null || username.trim().isEmpty()) {
            throw new BadRequestException("Không tìm thấy thông tin xác thực của người dùng");
        }
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người dùng: " + username));

        // Check if email changed and is taken by another user
        if (userRepository.existsByEmailAndIdNot(request.getEmail(), user.getId())) {
            throw new DuplicateResourceException("Email đã được sử dụng bởi tài khoản khác: " + request.getEmail());
        }

        user.setEmail(request.getEmail().trim());
        userRepository.save(user);

        // Update or create Customer Entity
        Optional<CustomerEntity> customerOpt = customerRepository.findByUserUsername(username);
        CustomerEntity customer;
        if (customerOpt.isPresent()) {
            customer = customerOpt.get();
            if (customerRepository.existsByEmailAndIdNot(request.getEmail(), customer.getId())) {
                throw new DuplicateResourceException("Email đã được sử dụng bởi khách hàng khác: " + request.getEmail());
            }
            customer.setName(request.getName().trim());
            customer.setEmail(request.getEmail().trim());
            customer.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
            customerRepository.save(customer);
        } else {
            customer = CustomerEntity.builder()
                    .name(request.getName().trim())
                    .email(request.getEmail().trim())
                    .phone(request.getPhone() != null ? request.getPhone().trim() : "")
                    .status(CustomerStatus.ACTIVE)
                    .deleted(false)
                    .user(user)
                    .build();
            customerRepository.save(customer);
        }

        return getProfile(username);
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        if (username == null || username.trim().isEmpty()) {
            throw new BadRequestException("Không tìm thấy thông tin xác thực của người dùng");
        }
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người dùng: " + username));

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp với mật khẩu mới");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu hiện tại không chính xác");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public UserResponse updateRole(Long id, UserRole role) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setRole(role);
        userRepository.save(user);
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateStatus(Long id, UserStatus status) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setStatus(status);
        userRepository.save(user);
        return mapToResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
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

