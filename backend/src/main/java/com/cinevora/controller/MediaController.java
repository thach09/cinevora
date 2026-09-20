package com.cinevora.controller;

import com.cinevora.common.ApiResponse;
import com.cinevora.dto.MovieDtos;
import com.cinevora.service.MediaService;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/media")
@PreAuthorize("hasRole('ADMIN')")
public class MediaController {
    private final MediaService service;

    public MediaController(MediaService service) { this.service = service; }

    @PostMapping(value = "/movies/{movieId}/poster", consumes = "multipart/form-data")
    public ApiResponse<MovieDtos.Response> uploadPoster(@PathVariable Long movieId, @RequestPart("file") @NotNull MultipartFile file) {
        return ApiResponse.ok("Poster đã được cập nhật", service.uploadPoster(movieId, file));
    }

    @DeleteMapping("/movies/{movieId}/poster")
    public ApiResponse<MovieDtos.Response> removePoster(@PathVariable Long movieId) {
        return ApiResponse.ok("Poster đã được gỡ", service.removePoster(movieId));
    }
}
