package com.example.ecommerce.auth.controller;

import com.example.ecommerce.auth.dto.request.LoginRequest;
import com.example.ecommerce.auth.dto.request.RegisterRequest;
import com.example.ecommerce.auth.dto.response.LoginResponse;
import com.example.ecommerce.auth.dto.response.UserResponse;
import com.example.ecommerce.auth.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
@RequiredArgsConstructor
@Tag(
        name = "Authentication",
        description = "APIs cho việc xác thực người dùng."
)
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @Operation(
            summary = "Đăng ký người dùng mới",
            description = "API cho phép đăng ký tài khoản customer và tạo đồng thời user + customer trong một transaction."
    )
    @PostMapping("/auth/register")
    public UserResponse register(@Valid @RequestBody RegisterRequest request){
        return authenticationService.register(request);
    }


    @Operation(
            summary = "Đăng nhập",
            description = "API cho phép người dùng đăng nhập với username và password, trả về JWT token."
    )
    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request){
        return authenticationService.login(request);
    }


    @Operation(
            summary = "Đăng xuất",
            description = "API cho phép người dùng đăng xuất yêu cầu auth."
    )
    @PostMapping(value = "/auth/logout")
    public void logout(@RequestHeader("Authorization") String bearerToken) throws ParseException {
        String token = bearerToken.replace("Bearer ", "");
        authenticationService.logout(token);
    }
}
