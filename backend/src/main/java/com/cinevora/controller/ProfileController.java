package com.cinevora.controller;

import com.cinevora.common.ApiResponse;
import com.cinevora.dto.ProfileDtos;
import com.cinevora.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/profiles")
public class ProfileController {
    private final ProfileService service;
    public ProfileController(ProfileService service) { this.service = service; }
    @GetMapping public ApiResponse<List<ProfileDtos.Response>> list(@AuthenticationPrincipal String username) { return ApiResponse.ok(service.list(username)); }
    @PostMapping public ApiResponse<ProfileDtos.Response> create(@AuthenticationPrincipal String username, @Valid @RequestBody ProfileDtos.Request request) { return ApiResponse.ok("Profile đã được tạo", service.create(username, request)); }
    @PutMapping("/{id}") public ApiResponse<ProfileDtos.Response> update(@AuthenticationPrincipal String username, @PathVariable Long id, @Valid @RequestBody ProfileDtos.Request request) { return ApiResponse.ok("Profile đã được cập nhật", service.update(username, id, request)); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@AuthenticationPrincipal String username, @PathVariable Long id) { service.delete(username, id); return ApiResponse.ok(null); }
    @PostMapping("/{id}/select") public ApiResponse<ProfileDtos.Response> select(@AuthenticationPrincipal String username, @PathVariable Long id) { return ApiResponse.ok(ProfileDtos.Response.from(service.resolve(username, id))); }
}
