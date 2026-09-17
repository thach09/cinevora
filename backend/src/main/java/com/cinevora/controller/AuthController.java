package com.cinevora.controller;

import com.cinevora.common.ApiResponse;
import com.cinevora.dto.AuthDtos;
import com.cinevora.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }
    @PostMapping("/login") public ApiResponse<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) { return ApiResponse.ok(auth.login(request)); }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) { return ApiResponse.ok("Đăng ký thành công", auth.register(request)); }
}
