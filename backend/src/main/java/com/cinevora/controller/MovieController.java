package com.cinevora.controller;

import com.cinevora.common.*;
import com.cinevora.dto.MovieDtos;
import com.cinevora.service.MovieService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/movies")
public class MovieController {
    private final MovieService service;
    public MovieController(MovieService service) { this.service = service; }
    @GetMapping public ApiResponse<PageResponse<MovieDtos.Response>> search(@RequestParam(required = false) String q, @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer minYear, @RequestParam(required = false) Integer maxYear, @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size, @RequestParam(defaultValue = "popularity") String sort,
            @RequestParam(defaultValue = "desc") String direction) { return ApiResponse.ok(service.search(q, categoryId, minYear, maxYear, minRating, page, size, sort, direction)); }
    @GetMapping("/trending") public ApiResponse<PageResponse<MovieDtos.Response>> trending(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) { return ApiResponse.ok(service.trending(page, size)); }
    @GetMapping("/{id}") public ApiResponse<MovieDtos.Response> get(@PathVariable Long id) { return ApiResponse.ok(service.getActive(id)); }
    @PostMapping @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<MovieDtos.Response> create(@Valid @RequestBody MovieDtos.Request request) { return ApiResponse.ok("Tạo phim thành công", service.create(request)); }
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public ApiResponse<MovieDtos.Response> update(@PathVariable Long id, @Valid @RequestBody MovieDtos.Request request) { return ApiResponse.ok("Cập nhật phim thành công", service.update(id, request)); }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public ApiResponse<Void> delete(@PathVariable Long id) { service.delete(id); return ApiResponse.ok(null); }
    @PatchMapping("/{id}/restore") @PreAuthorize("hasRole('ADMIN')") public ApiResponse<MovieDtos.Response> restore(@PathVariable Long id) { return ApiResponse.ok("Khôi phục phim thành công", service.restore(id)); }
}
