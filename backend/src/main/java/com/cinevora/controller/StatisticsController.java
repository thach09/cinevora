package com.cinevora.controller;

import com.cinevora.common.ApiResponse;
import com.cinevora.dto.StatisticsResponse;
import com.cinevora.service.StatisticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsController {
    private final StatisticsService service;
    public StatisticsController(StatisticsService service) { this.service = service; }
    @GetMapping @PreAuthorize("hasRole('ADMIN')") public ApiResponse<StatisticsResponse> get() { return ApiResponse.ok(service.get()); }
}
