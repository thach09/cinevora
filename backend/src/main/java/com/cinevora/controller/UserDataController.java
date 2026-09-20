package com.cinevora.controller;

import com.cinevora.common.ApiResponse;
import com.cinevora.dto.UserDataDtos;
import com.cinevora.service.UserDataService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserDataController {
    private final UserDataService service;
    public UserDataController(UserDataService service) { this.service = service; }
    @GetMapping("/watchlist") public ApiResponse<List<UserDataDtos.MovieRef>> watchlist(@AuthenticationPrincipal String username, @RequestHeader(value = "X-Profile-Id", required = false) Long profileId) { return ApiResponse.ok(service.getWatchlist(username, profileId)); }
    @PostMapping("/watchlist/{movieId}") public ApiResponse<Void> addWatchlist(@AuthenticationPrincipal String username, @RequestHeader(value = "X-Profile-Id", required = false) Long profileId, @PathVariable Long movieId) { service.addWatchlist(username, profileId, movieId); return ApiResponse.ok(null); }
    @DeleteMapping("/watchlist/{movieId}") public ApiResponse<Void> removeWatchlist(@AuthenticationPrincipal String username, @RequestHeader(value = "X-Profile-Id", required = false) Long profileId, @PathVariable Long movieId) { service.removeWatchlist(username, profileId, movieId); return ApiResponse.ok(null); }
    @GetMapping("/favourites") public ApiResponse<List<UserDataDtos.MovieRef>> favourites(@AuthenticationPrincipal String username, @RequestHeader(value = "X-Profile-Id", required = false) Long profileId) { return ApiResponse.ok(service.getFavourites(username, profileId)); }
    @PostMapping("/favourites/{movieId}") public ApiResponse<Void> addFavourite(@AuthenticationPrincipal String username, @RequestHeader(value = "X-Profile-Id", required = false) Long profileId, @PathVariable Long movieId) { service.addFavourite(username, profileId, movieId); return ApiResponse.ok(null); }
    @DeleteMapping("/favourites/{movieId}") public ApiResponse<Void> removeFavourite(@AuthenticationPrincipal String username, @RequestHeader(value = "X-Profile-Id", required = false) Long profileId, @PathVariable Long movieId) { service.removeFavourite(username, profileId, movieId); return ApiResponse.ok(null); }
    @GetMapping("/history") public ApiResponse<List<UserDataDtos.HistoryResponse>> history(@AuthenticationPrincipal String username, @RequestHeader(value = "X-Profile-Id", required = false) Long profileId) { return ApiResponse.ok(service.getHistory(username, profileId)); }
    @PostMapping("/history/{movieId}") public ApiResponse<Void> addHistory(@AuthenticationPrincipal String username, @RequestHeader(value = "X-Profile-Id", required = false) Long profileId, @PathVariable Long movieId) { service.addHistory(username, profileId, movieId); return ApiResponse.ok(null); }
    @GetMapping("/history/export") public ResponseEntity<byte[]> exportHistory(@AuthenticationPrincipal String username, @RequestHeader(value = "X-Profile-Id", required = false) Long profileId) { return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv; charset=UTF-8")).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=watch-history.csv").body(service.exportHistory(username, profileId)); }
    @GetMapping("/continue-watching") public ApiResponse<List<UserDataDtos.ContinueResponse>> getContinue(@AuthenticationPrincipal String username, @RequestHeader(value = "X-Profile-Id", required = false) Long profileId) { return ApiResponse.ok(service.getContinue(username, profileId)); }
    @PutMapping("/continue-watching") public ApiResponse<Void> updateContinue(@AuthenticationPrincipal String username, @RequestHeader(value = "X-Profile-Id", required = false) Long profileId, @Valid @RequestBody UserDataDtos.ProgressRequest request) { service.updateProgress(username, profileId, request); return ApiResponse.ok(null); }
    @DeleteMapping("/continue-watching/{movieId}") public ApiResponse<Void> removeContinue(@AuthenticationPrincipal String username, @RequestHeader(value = "X-Profile-Id", required = false) Long profileId, @PathVariable Long movieId) { service.removeContinue(username, profileId, movieId); return ApiResponse.ok(null); }
}
