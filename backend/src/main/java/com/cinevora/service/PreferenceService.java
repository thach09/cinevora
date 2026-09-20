package com.cinevora.service;
import com.cinevora.dto.DiscoveryDtos;
import com.cinevora.entity.*;
import com.cinevora.exception.ResourceNotFoundException;
import com.cinevora.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service public class PreferenceService {
    private final MovieRepository movies; private final MoviePreferenceRepository preferences; private final ProfileService profiles;
    public PreferenceService(MovieRepository movies, MoviePreferenceRepository preferences, ProfileService profiles) { this.movies = movies; this.preferences = preferences; this.profiles = profiles; }
    @Transactional(readOnly = true) public List<DiscoveryDtos.PreferenceResponse> list(String username, Long profileId) { return preferences.findByProfile_Id(profiles.resolve(username, profileId).getId()).stream().map(DiscoveryDtos.PreferenceResponse::from).toList(); }
    @Transactional public DiscoveryDtos.PreferenceResponse set(String username, Long profileId, Long movieId, DiscoveryDtos.PreferenceRequest request) { Profile profile = profiles.resolve(username, profileId); Movie movie = movies.findByIdAndActiveTrue(movieId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phim " + movieId)); PreferenceSignal signal = DiscoveryDtos.signal(request.signal()); MoviePreference preference = preferences.findByProfile_IdAndMovie_Id(profile.getId(), movieId).orElseGet(() -> new MoviePreference(profile, movie, signal)); preference.setSignal(signal); return DiscoveryDtos.PreferenceResponse.from(preferences.save(preference)); }
    @Transactional public void remove(String username, Long profileId, Long movieId) { preferences.deleteById(new MoviePreferenceId(profiles.resolve(username, profileId).getId(), movieId)); }
}
