package com.cinevora.controller;

import com.cinevora.common.ApiResponse;
import com.cinevora.common.PageResponse;
import com.cinevora.dto.AdminDtos;
import com.cinevora.dto.UserResponse;
import com.cinevora.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final AdminUserService service;
    public AdminUserController(AdminUserService service) { this.service = service; }

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> search(@RequestParam(required = false) String q,
                                                            @RequestParam(defaultValue = "all") String status,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.search(q, status, page, size));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<UserResponse> setStatus(@AuthenticationPrincipal String username, @PathVariable Long id,
                                                @Valid @RequestBody AdminDtos.ActiveRequest request) {
        return ApiResponse.ok(service.setStatus(username, id, request));
    }
}
