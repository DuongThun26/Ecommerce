package com.example.ecommerce.customer.service.impl;

import com.example.ecommerce.auth.entity.AccountActivationTokenEntity;
import com.example.ecommerce.auth.entity.UserEntity;
import com.example.ecommerce.auth.enums.UserRole;
import com.example.ecommerce.auth.enums.UserStatus;
import com.example.ecommerce.auth.repository.AccountActivationTokenRepository;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.common.exception.DuplicateResourceException;
import com.example.ecommerce.common.exception.ResourceInUseException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.common.util.UsernameGenerator;
import com.example.ecommerce.customer.dto.request.CustomerRequest;
import com.example.ecommerce.customer.dto.response.CustomerResponse;
import com.example.ecommerce.customer.dto.response.CustomerResponseSummary;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.customer.enums.CustomerStatus;
import com.example.ecommerce.customer.mapper.CustomerMapper;
import com.example.ecommerce.customer.repository.CustomerRepository;
import com.example.ecommerce.customer.service.CustomerService;
import com.example.ecommerce.mail.service.EmailService;
import com.example.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final CustomerMapper customerMapper;
    private final UserRepository userRepository;
    private final AccountActivationTokenRepository accountActivationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl = "http://localhost:3000";

    @Value("${app.invoice.store-name:E-COMMERCE STORE}")
    private String storeName = "E-COMMERCE STORE";

    @Override
    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

        if (customerRepository.existsByEmail(email) || userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Khách hàng hoặc tài khoản với email " + email + " đã tồn tại.");
        }

        // 1. Sinh username ngẫu nhiên duy nhất
        String username = UsernameGenerator.generateUniqueUsername(request.getName(), userRepository::existsByUsername);

        // 2. Tạo UserEntity ở trạng thái PENDING_ACTIVATION
        String randomPassword = UUID.randomUUID().toString();
        UserEntity user = UserEntity.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(randomPassword))
                .role(UserRole.CUSTOMER)
                .status(UserStatus.PENDING_ACTIVATION)
                .build();
        userRepository.save(user);

        // 3. Tạo CustomerEntity ở trạng thái INACTIVE (chờ kích hoạt)
        CustomerEntity customer = customerMapper.toEntity(request);
        customer.setEmail(email);
        customer.setStatus(CustomerStatus.INACTIVE);
        customer.setDeleted(false);
        customer.setUser(user);
        customerRepository.save(customer);

        // 4. Tạo token kích hoạt tài khoản có hạn 24 giờ
        String token = UUID.randomUUID().toString();
        AccountActivationTokenEntity activationToken = AccountActivationTokenEntity.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .build();
        accountActivationTokenRepository.save(activationToken);

        // 5. Gửi email thông báo username và đường dẫn thiết lập mật khẩu
        sendActivationEmail(user, customer.getName(), token);

        return customerMapper.toResponse(customer);
    }

    @Override
    public List<CustomerResponse> getAllCustomers() {
        List<CustomerEntity> customers = customerRepository.findAllByDeletedFalse();
        return customerMapper.toResponseList(customers);
    }

    @Override
    public CustomerResponse getCustomerById(Long id) {
        CustomerEntity customer = customerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + id + " not found."));
        return customerMapper.toResponse(customer);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        CustomerEntity customer = customerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + id + " not found."));

        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        Long linkedUserId = customer.getUser() != null ? customer.getUser().getId() : -1L;

        if (customerRepository.existsByEmailAndIdNot(email, id) || userRepository.existsByEmailAndIdNot(email, linkedUserId)) {
            throw new DuplicateResourceException("Khách hàng hoặc tài khoản với email " + email + " đã tồn tại.");
        }

        customerMapper.updateEntity(request, customer);
        customer.setEmail(email);

        if (customer.getUser() != null) {
            customer.getUser().setEmail(email);
            userRepository.save(customer.getUser());
        }

        customerRepository.save(customer);
        return customerMapper.toResponse(customer);
    }

    @Override
    @Transactional
    public void deleteCustomerById(Long id) {
        CustomerEntity customer = customerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + id + " not found."));
        if (orderRepository.existsByCustomerId(id)) {
            throw new ResourceInUseException("Customer with id " + id + " already has orders and cannot be deleted.");
        }
        customer.setDeleted(true);
        customer.setStatus(CustomerStatus.INACTIVE);
        customerRepository.save(customer);

        if (customer.getUser() != null) {
            customer.getUser().setStatus(UserStatus.INACTIVE);
            userRepository.save(customer.getUser());
        }
    }

    @Override
    @Transactional
    public void resendActivationEmail(Long customerId) {
        CustomerEntity customer = customerRepository.findByIdAndDeletedFalse(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + customerId + " not found."));

        UserEntity user = customer.getUser();
        if (user == null) {
            throw new BadRequestException("Khách hàng chưa được liên kết tài khoản hệ thống.");
        }

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BadRequestException("Tài khoản đã được kích hoạt thành công, không thể gửi lại liên kết kích hoạt.");
        }

        String token = UUID.randomUUID().toString();
        AccountActivationTokenEntity activationToken = AccountActivationTokenEntity.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .build();
        accountActivationTokenRepository.save(activationToken);

        sendActivationEmail(user, customer.getName(), token);
    }

    private void sendActivationEmail(UserEntity user, String customerName, String token) {
        try {
            Context context = new Context();
            context.setVariable("customerName", customerName != null ? customerName : user.getUsername());
            context.setVariable("username", user.getUsername());
            context.setVariable("email", user.getEmail());
            context.setVariable("activationUrl", frontendUrl + "/set-password?token=" + token);
            context.setVariable("expiresInHours", 24);
            context.setVariable("storeName", storeName);

            String contentHtml = templateEngine.process("mail/customer-activation", context);
            emailService.sendEmail(user.getEmail(), "[" + storeName + "] Kích hoạt tài khoản và thiết lập mật khẩu", contentHtml);
        } catch (Exception ex) {
            log.error("Lỗi khi gửi email kích hoạt cho user {}: {}", user.getUsername(), ex.getMessage(), ex);
        }
    }
}
