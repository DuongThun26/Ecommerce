package com.example.ecommerce.testdata;

import com.example.ecommerce.auth.dto.request.LoginRequest;
import com.example.ecommerce.product.dto.request.ProductRequest;
import com.example.ecommerce.auth.dto.request.RegisterRequest;
import com.example.ecommerce.auth.dto.request.UserRequest;
import com.example.ecommerce.product.dto.response.ProductResponse;
import com.example.ecommerce.auth.dto.response.UserResponse;
import com.example.ecommerce.product.enums.ProductStatus;

import java.math.BigDecimal;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static LoginRequest createLoginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    public static RegisterRequest createRegisterRequest(String username, String email, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        request.setName("Test Name");
        request.setPhone("0900000000");
        return request;
    }

    public static UserRequest createUserRequest(String username, String email, String password) {
        UserRequest request = new UserRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        request.setName("Test Name");
        request.setPhone("0900000000");
        return request;
    }

    public static ProductRequest createProductRequest(String name, BigDecimal price, Integer stock) {
        ProductRequest request = new ProductRequest();
        request.setName(name);
        request.setPrice(price);
        request.setStock(stock);
        request.setStatus(ProductStatus.ACTIVE);
        return request;
    }

    public static ProductResponse createProductResponse(Long id, String name, BigDecimal price, Integer stock) {
        return ProductResponse.builder()
                .id(id)
                .name(name)
                .price(price)
                .stock(stock)
                .status(ProductStatus.ACTIVE)
                .build();
    }

    public static UserResponse createUserResponse(Long id, Long customerId) {
        return UserResponse.builder()
                .id(id)
                .customerId(customerId)
                .build();
    }
}
