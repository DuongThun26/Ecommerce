package com.example.ecommerce.auth.controller;

import com.example.ecommerce.auth.dto.request.UserRequest;
import com.example.ecommerce.auth.dto.response.UserResponse;
import com.example.ecommerce.auth.service.UserService;
import com.example.ecommerce.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody UserRequest request){
        UserResponse user = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo người dùng thành công", user));
    }

    @Operation(
            summary = "Lấy danh sách người dùng",
            description = "API cho phép lấy danh sách tất cả người dùng trong hệ thống yêu cầu quyền Admin."
    )
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAll(){
        List<UserResponse> users = userService.getAll();
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy danh sách người dùng thành công", users));
    }

    @Operation(
            summary = "Lấy chi tiết người dùng",
            description = "API cho phép lấy chi tiết người dùng theo ID."
    )
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@org.springframework.web.bind.annotation.PathVariable Long id) {
        UserResponse user = userService.getById(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy thông tin người dùng thành công", user));
    }

    @Operation(
            summary = "Phân quyền / Cập nhật Role người dùng",
            description = "API cho phép Admin thay đổi vai trò (ADMIN, PRODUCT_OWNER, CUSTOMER) của người dùng."
    )
    @org.springframework.web.bind.annotation.PatchMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateRole(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @Valid @RequestBody com.example.ecommerce.auth.dto.request.RoleUpdateRequest request) {
        UserResponse user = userService.updateRole(id, request.getRole());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Cập nhật quyền vai trò người dùng thành công", user));
    }

    @Operation(
            summary = "Cập nhật trạng thái người dùng",
            description = "API cho phép Admin khóa hoặc mở khóa tài khoản (ACTIVE, INACTIVE)."
    )
    @org.springframework.web.bind.annotation.PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @Valid @RequestBody com.example.ecommerce.auth.dto.request.UserStatusUpdateRequest request) {
        UserResponse user = userService.updateStatus(id, request.getStatus());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Cập nhật trạng thái người dùng thành công", user));
    }

    @Operation(
            summary = "Vô hiệu hóa / Xóa người dùng",
            description = "API cho phép Admin vô hiệu hóa người dùng."
    )
    @org.springframework.web.bind.annotation.DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @org.springframework.web.bind.annotation.PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Vô hiệu hóa người dùng thành công", null));
    }
}
