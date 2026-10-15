package com.example.ecommerce.auth.controller;

import com.example.ecommerce.auth.dto.request.LoginRequest;
import com.example.ecommerce.auth.dto.request.RefreshTokenRequest;
import com.example.ecommerce.auth.dto.request.RegisterRequest;
import com.example.ecommerce.auth.dto.response.LoginResponse;
import com.example.ecommerce.auth.dto.response.UserResponse;
import com.example.ecommerce.auth.entity.UserEntity;
import com.example.ecommerce.auth.service.AuthenticationService;
import com.example.ecommerce.common.response.ApiResponse;
import com.nimbusds.jose.JOSEException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request){
        UserResponse user = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng kí thành công", user));
    }


    @Operation(
            summary = "Đăng nhập",
            description = "API cho phép người dùng đăng nhập với username và password, trả về JWT token."
    )
    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request){
        LoginResponse login = authenticationService.login(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Đăng nhập thành công", login));
    }


    @Operation(
            summary = "Đăng xuất",
            description = "API cho phép người dùng đăng xuất yêu cầu auth."
    )
    @PostMapping(value = "/auth/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String bearerToken) throws ParseException {
        String token = bearerToken.replace("Bearer ", "");
        authenticationService.logout(token);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Đăng xuất thành công", null));
    }

    @Operation(
            summary = "Refresh token",
            description = "Api cho phép người dung lấy access token mới khi refresh token còn hạn."
    )
    @PostMapping(value = "/auth/refresh-token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@RequestHeader("Authorization") String bearerToken) throws ParseException, JOSEException {
        String token = bearerToken.replace("Bearer ", "");
        LoginResponse loginResponse = authenticationService.refresh(RefreshTokenRequest.builder().refreshToken(token).build());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Lấy access token mới thành công", loginResponse));
    }

    @Operation(
            summary = "Xác minh token kích hoạt tài khoản",
            description = "API kiểm tra tính hợp lệ của token kích hoạt và trả về thông tin tài khoản."
    )
    @org.springframework.web.bind.annotation.GetMapping("/auth/activation/verify")
    public ResponseEntity<ApiResponse<com.example.ecommerce.auth.dto.response.ActivationVerifyResponse>> verifyActivationToken(
            @org.springframework.web.bind.annotation.RequestParam("token") String token) {
        com.example.ecommerce.auth.dto.response.ActivationVerifyResponse response = authenticationService.verifyActivationToken(token);
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra token kích hoạt thành công", response));
    }

    @Operation(
            summary = "Thiết lập mật khẩu và kích hoạt tài khoản",
            description = "API cho phép khách hàng tự thiết lập mật khẩu bằng token kích hoạt."
    )
    @PostMapping("/auth/activation/set-password")
    public ResponseEntity<ApiResponse<Void>> setPasswordAndActivate(
            @Valid @RequestBody com.example.ecommerce.auth.dto.request.SetPasswordRequest request) {
        authenticationService.setPasswordAndActivate(request);
        return ResponseEntity.ok(ApiResponse.success("Thiết lập mật khẩu và kích hoạt tài khoản thành công", null));
    }
}
