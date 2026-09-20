package com.cinevora.controller;

import com.cinevora.common.ApiResponse;
import com.cinevora.dto.AccountDtos;
import com.cinevora.dto.UserResponse;
import com.cinevora.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me")
public class AccountController {
    private final AccountService service;
    public AccountController(AccountService service) { this.service = service; }
    @GetMapping public ApiResponse<UserResponse> get(@AuthenticationPrincipal String username) { return ApiResponse.ok(service.get(username)); }
    @PutMapping public ApiResponse<UserResponse> update(@AuthenticationPrincipal String username, @Valid @RequestBody AccountDtos.UpdateProfileRequest request) { return ApiResponse.ok("Hồ sơ đã được cập nhật", service.update(username, request)); }
    @PutMapping("/password") public ApiResponse<Void> changePassword(@AuthenticationPrincipal String username, @Valid @RequestBody AccountDtos.ChangePasswordRequest request) { service.changePassword(username, request); return ApiResponse.ok("Mật khẩu đã được cập nhật", null); }
    @GetMapping("/sessions") public ApiResponse<List<AccountDtos.SessionResponse>> sessions(@AuthenticationPrincipal String username) { return ApiResponse.ok(service.sessions(username)); }
    @DeleteMapping("/sessions/{id}") public ApiResponse<Void> revokeSession(@AuthenticationPrincipal String username, @PathVariable Long id) { service.revokeSession(username, id); return ApiResponse.ok(null); }
}
