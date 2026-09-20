package com.cinevora.controller;

import com.cinevora.common.ApiResponse;
import com.cinevora.dto.NotificationDtos;
import com.cinevora.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }
    @GetMapping public ApiResponse<NotificationDtos.Inbox> inbox(@AuthenticationPrincipal String username) { return ApiResponse.ok(service.inbox(username)); }
    @PatchMapping("/{id}/read") public ApiResponse<Void> markRead(@AuthenticationPrincipal String username, @PathVariable Long id) { service.markRead(username, id); return ApiResponse.ok(null); }
    @PostMapping("/read-all") public ApiResponse<Void> markAllRead(@AuthenticationPrincipal String username) { service.markAllRead(username); return ApiResponse.ok(null); }
}
