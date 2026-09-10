package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.LoginRequest;
import com.example.ecommerce.dto.request.UserRequest;
import com.example.ecommerce.dto.response.LoginResponse;
import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.service.AuthenticationService;
import com.example.ecommerce.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final UserService userService;

    @PostMapping("/auth/register")
    public UserResponse register(@Valid @RequestBody UserRequest request){
        return userService.create(request);
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request){
        return authenticationService.login(request);
    }

    @PostMapping(value = "/auth/logout")
    public void logout(@RequestHeader("Authorization") String bearerToken) throws ParseException {
        String token = bearerToken.replace("Bearer ", "");
        authenticationService.logout(token);
    }
}
