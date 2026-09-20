package com.cinevora.controller;

import com.cinevora.common.ApiResponse;
import com.cinevora.dto.MediaTrackDtos;
import com.cinevora.service.MediaTrackService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class MediaTrackController {
    private final MediaTrackService service;
    public MediaTrackController(MediaTrackService service) { this.service = service; }
    @GetMapping("/movies/{movieId}/tracks") public ApiResponse<List<MediaTrackDtos.Response>> publicList(@PathVariable Long movieId) { return ApiResponse.ok(service.publicList(movieId)); }
    @GetMapping("/admin/movies/{movieId}/tracks") @PreAuthorize("hasRole('ADMIN')") public ApiResponse<List<MediaTrackDtos.Response>> adminList(@PathVariable Long movieId) { return ApiResponse.ok(service.adminList(movieId)); }
    @PostMapping("/admin/movies/{movieId}/tracks") @PreAuthorize("hasRole('ADMIN')") public ApiResponse<MediaTrackDtos.Response> create(@PathVariable Long movieId, @Valid @RequestBody MediaTrackDtos.Request request) { return ApiResponse.ok(service.create(movieId, request)); }
    @DeleteMapping("/admin/movies/{movieId}/tracks/{trackId}") @PreAuthorize("hasRole('ADMIN')") public ApiResponse<Void> archive(@PathVariable Long movieId, @PathVariable Long trackId) { service.archive(movieId, trackId); return ApiResponse.ok(null); }
}
