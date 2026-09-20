package com.cinevora.controller;

import com.cinevora.common.ApiResponse;
import com.cinevora.common.PageResponse;
import com.cinevora.dto.AdminDtos;
import com.cinevora.dto.MovieDtos;
import com.cinevora.service.MovieService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/movies")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMovieController {
    private final MovieService service;
    public AdminMovieController(MovieService service) { this.service = service; }

    @GetMapping
    public ApiResponse<PageResponse<MovieDtos.Response>> search(@RequestParam(required = false) String q,
                                                                  @RequestParam(required = false) Long categoryId,
                                                                  @RequestParam(defaultValue = "true") boolean includeInactive,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "12") int size,
                                                                  @RequestParam(defaultValue = "popularity") String sort,
                                                                  @RequestParam(defaultValue = "desc") String direction) {
        return ApiResponse.ok(service.searchAdmin(q, categoryId, includeInactive, page, size, sort, direction));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<MovieDtos.Response> setStatus(@PathVariable Long id, @Valid @RequestBody AdminDtos.ActiveRequest request) {
        return ApiResponse.ok(service.setActive(id, request.active()));
    }
}
