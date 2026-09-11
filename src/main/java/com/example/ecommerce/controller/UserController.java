package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.UserRequest;
import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(
        name = "User",
        description = "APIs cho việc quản lý người dùng."
)

public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Tạo nguời dùng mới",
            description = "API cho phép tạo mới người dùng với thông tin được cung cấp.",
            security = {}
    )
    @PostMapping("/users")
    public UserResponse create(@Valid @RequestBody UserRequest request){
        return userService.create(request);
    }

    @Operation(
            summary = "Lấy danh sách người dùng",
            description = "API cho phép lấy danh sách tất cả người dùng trong hệ thống yêu cầu quyền Admin."
    )
    @GetMapping("/users")
    public List<UserResponse> getAll(){
        return userService.getAll();
    }
}
