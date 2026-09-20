package com.cinevora.controller;

import com.cinevora.common.ApiResponse;
import com.cinevora.dto.AdminDtos;
import com.cinevora.dto.CategoryDtos;
import com.cinevora.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {
    private final CategoryService service;
    public AdminCategoryController(CategoryService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<CategoryDtos.Response>> list(@RequestParam(defaultValue = "true") boolean includeInactive) {
        return ApiResponse.ok(service.list(includeInactive));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<CategoryDtos.Response> setStatus(@PathVariable Long id, @Valid @RequestBody AdminDtos.ActiveRequest request) {
        return ApiResponse.ok(service.setActive(id, request.active()));
    }
}
