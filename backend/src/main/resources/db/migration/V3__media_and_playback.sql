ALTER TABLE continue_watching
    ADD COLUMN position_seconds INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN duration_seconds INTEGER;

UPDATE continue_watching cw
SET duration_seconds = m.duration_minutes * 60,
    position_seconds = CASE
        WHEN m.duration_minutes IS NULL THEN 0
        ELSE ROUND((cw.percent / 100.0) * (m.duration_minutes * 60))::INTEGER
    END
FROM movies m
WHERE m.id = cw.movie_id;

CREATE INDEX idx_continue_watching_user_updated_at
    ON continue_watching (user_id, updated_at DESC);
