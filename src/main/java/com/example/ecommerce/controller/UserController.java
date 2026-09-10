package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.UserRequest;
import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/users")
    public UserResponse create(@RequestBody UserRequest request){
        return userService.create(request);
    }
}
