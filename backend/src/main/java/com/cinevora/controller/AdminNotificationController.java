package com.cinevora.controller;

import com.cinevora.common.ApiResponse;
import com.cinevora.dto.NotificationDtos;
import com.cinevora.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {
    private final NotificationService service;
    public AdminNotificationController(NotificationService service) { this.service = service; }

    @PostMapping
    public ApiResponse<NotificationDtos.DispatchResponse> send(@Valid @RequestBody NotificationDtos.AdminMessageRequest request) {
        return ApiResponse.ok("Notification sent", service.sendAdminMessage(request));
    }
}
