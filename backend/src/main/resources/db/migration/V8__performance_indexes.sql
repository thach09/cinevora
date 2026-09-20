-- Cover the hottest Phase 3.8/3.9 paths without changing API semantics.
CREATE INDEX idx_movies_active_views_id ON movies (is_active, views DESC, id);
CREATE INDEX idx_users_active_created ON users (is_active, created_at DESC, id);
CREATE INDEX idx_watch_history_profile_movie ON watch_history (profile_id, movie_id);
CREATE INDEX idx_movie_preferences_movie_signal ON movie_preferences (movie_id, signal);
CREATE INDEX idx_search_history_query_searched ON search_history (query_text, searched_at DESC);
