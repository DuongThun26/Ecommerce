package com.example.ecommerce.entity;

import com.example.ecommerce.dto.request.LoginRequest;
import com.example.ecommerce.dto.response.LoginResponse;
import com.example.ecommerce.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/auth/login")
    public LoginResponse login(@RequestBody LoginRequest request){
        return authenticationService.login(request);
    }
}
