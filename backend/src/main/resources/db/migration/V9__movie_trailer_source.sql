-- Keep promotional trailers separate from the playable source. Trailer playback
-- must never participate in watch progress, history, or Continue Watching.
ALTER TABLE movies ADD COLUMN trailer_url VARCHAR(500);

-- Existing catalogue rows were populated by the legal trailer importer before
-- the two playback modes existed. Move only unambiguous trailer URLs; leave
-- anything else available for manual review.
UPDATE movies
SET trailer_url = video_url,
    video_url = NULL
WHERE trailer_url IS NULL
  AND video_url IS NOT NULL
  AND (
      lower(video_url) LIKE '%youtube.com%'
      OR lower(video_url) LIKE '%youtu.be%'
      OR lower(video_url) LIKE '%youtube-nocookie.com%'
      OR lower(video_url) LIKE '%/phim-bong-de-trailer%'
  );
