package com.cinevora.controller;

import com.cinevora.common.ApiResponse;
import com.cinevora.dto.AuthDtos;
import com.cinevora.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }
    @PostMapping("/login") public ApiResponse<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request, HttpServletRequest http) { return ApiResponse.ok(auth.login(request, http.getHeader("User-Agent"), http.getRemoteAddr())); }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request, HttpServletRequest http) { return ApiResponse.ok("Đăng ký thành công", auth.register(request, http.getHeader("User-Agent"), http.getRemoteAddr())); }
    @PostMapping("/refresh") public ApiResponse<AuthDtos.AuthResponse> refresh(@Valid @RequestBody AuthDtos.RefreshRequest request, HttpServletRequest http) { return ApiResponse.ok(auth.refresh(request, http.getHeader("User-Agent"), http.getRemoteAddr())); }
    @PostMapping("/logout") public ApiResponse<Void> logout(@RequestBody(required = false) AuthDtos.LogoutRequest request) { auth.logout(request == null ? new AuthDtos.LogoutRequest(null) : request); return ApiResponse.ok(null); }
    @PostMapping("/forgot-password") public ApiResponse<AuthDtos.GenericTokenResponse> forgotPassword(@Valid @RequestBody AuthDtos.ForgotPasswordRequest request) { return ApiResponse.ok(auth.forgotPassword(request)); }
    @PostMapping("/reset-password") public ApiResponse<Void> resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) { auth.resetPassword(request); return ApiResponse.ok(null); }
    @PostMapping("/verify-email") public ApiResponse<Void> verifyEmail(@Valid @RequestBody AuthDtos.VerifyEmailRequest request) { auth.verifyEmail(request); return ApiResponse.ok(null); }
}
