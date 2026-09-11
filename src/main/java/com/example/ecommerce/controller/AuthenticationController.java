package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.LoginRequest;
import com.example.ecommerce.dto.request.UserRequest;
import com.example.ecommerce.dto.response.LoginResponse;
import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.service.AuthenticationService;
import com.example.ecommerce.service.UserService;
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
    private final UserService userService;

    @Operation(
            summary = "Đăng ký người dùng mới",
            description = "API cho phép người dùng đăng ký với username, password."
    )
    @PostMapping("/auth/register")
    public UserResponse register(@Valid @RequestBody UserRequest request){
        return userService.create(request);
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
