package com.example.ecommerce.controller;

import com.example.ecommerce.auth.controller.AuthenticationController;
import com.example.ecommerce.auth.dto.request.LoginRequest;
import com.example.ecommerce.auth.dto.request.RegisterRequest;
import com.example.ecommerce.auth.dto.response.LoginResponse;
import com.example.ecommerce.auth.dto.response.UserResponse;
import com.example.ecommerce.auth.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @Test
    @DisplayName("POST /auth/register should return wrapped success")
    void registerShouldReturnSuccess() throws Exception {
        UserResponse response = UserResponse.builder().id(1L).customerId(10L).build();
        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(response);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("tester");
        request.setPassword("password123");
        request.setName("Test");
        request.setEmail("tester@example.com");
        request.setPhone("0900000000");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        verify(authenticationService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login should return token payload")
    void loginShouldReturnToken() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .build();
        when(authenticationService.login(any(LoginRequest.class))).thenReturn(response);

        LoginRequest request = new LoginRequest();
        request.setUsername("tester");
        request.setPassword("password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        verify(authenticationService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /auth/logout should call service")
    void logoutShouldCallService() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer token-value"))
                .andExpect(status().isOk());

        verify(authenticationService).logout("token-value");
    }
}
