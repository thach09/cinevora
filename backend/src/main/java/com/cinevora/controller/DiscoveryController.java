package com.cinevora.controller;
import com.cinevora.common.ApiResponse;
import com.cinevora.dto.DiscoveryDtos;
import com.cinevora.service.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/v1") public class DiscoveryController {
    private final PreferenceService preferences; private final RecommendationService recommendations; private final SearchService search;
    public DiscoveryController(PreferenceService preferences, RecommendationService recommendations, SearchService search) { this.preferences = preferences; this.recommendations = recommendations; this.search = search; }
    @GetMapping("/users/me/preferences") public ApiResponse<List<DiscoveryDtos.PreferenceResponse>> preferences(@AuthenticationPrincipal String username, @RequestHeader(value="X-Profile-Id", required=false) Long profileId) { return ApiResponse.ok(preferences.list(username, profileId)); }
    @PutMapping("/users/me/preferences/{movieId}") public ApiResponse<DiscoveryDtos.PreferenceResponse> preference(@AuthenticationPrincipal String username, @RequestHeader(value="X-Profile-Id", required=false) Long profileId, @PathVariable Long movieId, @Valid @RequestBody DiscoveryDtos.PreferenceRequest request) { return ApiResponse.ok(preferences.set(username, profileId, movieId, request)); }
    @DeleteMapping("/users/me/preferences/{movieId}") public ApiResponse<Void> removePreference(@AuthenticationPrincipal String username, @RequestHeader(value="X-Profile-Id", required=false) Long profileId, @PathVariable Long movieId) { preferences.remove(username, profileId, movieId); return ApiResponse.ok(null); }
    @GetMapping("/recommendations") public ApiResponse<List<com.cinevora.dto.MovieDtos.Response>> recommendations(@AuthenticationPrincipal String username, @RequestHeader(value="X-Profile-Id", required=false) Long profileId, @RequestParam(defaultValue="12") int limit) { return ApiResponse.ok(recommendations.recommend(username, profileId, limit)); }
    @GetMapping("/home") public ApiResponse<DiscoveryDtos.HomeResponse> home(@AuthenticationPrincipal String username, @RequestHeader(value="X-Profile-Id", required=false) Long profileId) { return ApiResponse.ok(recommendations.home(username, profileId)); }
    @GetMapping("/users/me/search-history") public ApiResponse<List<DiscoveryDtos.SearchEntry>> recent(@AuthenticationPrincipal String username, @RequestHeader(value="X-Profile-Id", required=false) Long profileId) { return ApiResponse.ok(search.recent(username, profileId)); }
    @PostMapping("/users/me/search-history") public ApiResponse<Void> save(@AuthenticationPrincipal String username, @RequestHeader(value="X-Profile-Id", required=false) Long profileId, @RequestParam String q) { search.save(username, profileId, q); return ApiResponse.ok(null); }
    @DeleteMapping("/users/me/search-history") public ApiResponse<Void> clear(@AuthenticationPrincipal String username, @RequestHeader(value="X-Profile-Id", required=false) Long profileId) { search.clear(username, profileId); return ApiResponse.ok(null); }
    @GetMapping("/movies/popular-searches") public ApiResponse<List<DiscoveryDtos.PopularSearch>> popular(@RequestParam(defaultValue="6") int limit) { return ApiResponse.ok(search.popular(limit)); }
}
