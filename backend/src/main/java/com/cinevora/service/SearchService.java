package com.cinevora.service;
import com.cinevora.dto.DiscoveryDtos;
import com.cinevora.entity.Profile;
import com.cinevora.entity.SearchHistory;
import com.cinevora.repository.SearchHistoryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service public class SearchService {
    private final ProfileService profiles; private final SearchHistoryRepository history;
    public SearchService(ProfileService profiles, SearchHistoryRepository history) { this.profiles = profiles; this.history = history; }
    @Transactional(readOnly = true) public List<DiscoveryDtos.SearchEntry> recent(String username, Long profileId) { return history.findTop20ByProfile_IdOrderBySearchedAtDesc(profiles.resolve(username, profileId).getId()).stream().map(h -> new DiscoveryDtos.SearchEntry(h.getId(), h.getQuery(), h.getSearchedAt())).toList(); }
    @Transactional public void save(String username, Long profileId, String query) { String value = query == null ? "" : query.trim().replaceAll("\\s+", " "); if (value.length() < 2) return; Profile profile = profiles.resolve(username, profileId); history.save(new SearchHistory(profile, value.substring(0, Math.min(120, value.length())))); List<SearchHistory> rows = history.findAllRecent(profile.getId()); if (rows.size() > 20) history.deleteAll(rows.subList(20, rows.size())); }
    @Transactional public void clear(String username, Long profileId) { history.deleteAll(history.findAllRecent(profiles.resolve(username, profileId).getId())); }
    @Transactional(readOnly = true) public List<DiscoveryDtos.PopularSearch> popular(int limit) { return history.findPopularQueries(PageRequest.of(0, Math.max(1, Math.min(limit, 10)))).stream().map(row -> new DiscoveryDtos.PopularSearch((String) row[0], ((Number) row[1]).longValue())).toList(); }
}
