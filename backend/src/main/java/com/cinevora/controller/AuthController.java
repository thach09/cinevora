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
    private final com.cinevora.security.RefreshCookiePolicy cookies;
    public AuthController(AuthService auth, com.cinevora.security.RefreshCookiePolicy cookies) { this.auth = auth; this.cookies = cookies; }
    @GetMapping("/csrf") public ApiResponse<java.util.Map<String, Object>> csrf(org.springframework.security.web.csrf.CsrfToken token,
            @CookieValue(name = com.cinevora.security.RefreshCookiePolicy.NAME, required = false) String refresh) {
        return ApiResponse.ok(java.util.Map.of("token", token.getToken(), "headerName", token.getHeaderName(), "hasSession", refresh != null));
    }
    @PostMapping("/login") public ApiResponse<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request, HttpServletRequest http, jakarta.servlet.http.HttpServletResponse response) { var result = auth.login(request, http.getHeader("User-Agent"), http.getRemoteAddr()); cookies.write(response, result.refreshToken()); return ApiResponse.ok(result); }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request, HttpServletRequest http, jakarta.servlet.http.HttpServletResponse response) { var result = auth.register(request, http.getHeader("User-Agent"), http.getRemoteAddr()); cookies.write(response, result.refreshToken()); return ApiResponse.ok(result); }
    @PostMapping("/refresh") public ApiResponse<AuthDtos.AuthResponse> refresh(
            @CookieValue(name = com.cinevora.security.RefreshCookiePolicy.NAME, required = false) String token,
            HttpServletRequest http, jakarta.servlet.http.HttpServletResponse response) {
        var result = auth.refresh(new AuthDtos.RefreshRequest(token), http.getHeader("User-Agent"), http.getRemoteAddr());
        cookies.write(response, result.refreshToken()); return ApiResponse.ok(result);
    }
    @PostMapping("/logout") public ApiResponse<Void> logout(
            @CookieValue(name = com.cinevora.security.RefreshCookiePolicy.NAME, required = false) String token,
            jakarta.servlet.http.HttpServletResponse response) {
        auth.logout(new AuthDtos.LogoutRequest(token)); cookies.write(response, null); return ApiResponse.ok(null);
    }
    @PostMapping("/forgot-password") public ApiResponse<AuthDtos.GenericTokenResponse> forgotPassword(@Valid @RequestBody AuthDtos.ForgotPasswordRequest request) { return ApiResponse.ok(auth.forgotPassword(request)); }
    @PostMapping("/reset-password") public ApiResponse<Void> resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) { auth.resetPassword(request); return ApiResponse.ok(null); }
    @PostMapping("/verify-email") public ApiResponse<Void> verifyEmail(@Valid @RequestBody AuthDtos.VerifyEmailRequest request) { auth.verifyEmail(request); return ApiResponse.ok(null); }
}
