package com.cinevora.controller;

import com.cinevora.common.ApiResponse;
import com.cinevora.dto.CategoryDtos;
import com.cinevora.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService service;
    public CategoryController(CategoryService service) { this.service = service; }
    @GetMapping public ApiResponse<List<CategoryDtos.Response>> list() { return ApiResponse.ok(service.list(false)); }
    @PostMapping @PreAuthorize("hasRole('ADMIN')") public ApiResponse<CategoryDtos.Response> create(@Valid @RequestBody CategoryDtos.Request request) { return ApiResponse.ok("Tạo thể loại thành công", service.create(request)); }
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public ApiResponse<CategoryDtos.Response> update(@PathVariable Long id, @Valid @RequestBody CategoryDtos.Request request) { return ApiResponse.ok("Cập nhật thể loại thành công", service.update(id, request)); }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public ApiResponse<Void> delete(@PathVariable Long id) { service.delete(id); return ApiResponse.ok(null); }
    @PatchMapping("/{id}/restore") @PreAuthorize("hasRole('ADMIN')") public ApiResponse<CategoryDtos.Response> restore(@PathVariable Long id) { return ApiResponse.ok("Khôi phục thể loại thành công", service.restore(id)); }
}
