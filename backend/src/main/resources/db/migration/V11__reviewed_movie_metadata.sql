-- Reviewed seed enrichment. V1/V2 remain immutable.
-- Every predicate is a V2 fingerprint plus a missing synopsis guard: an admin edit skips that row.

UPDATE movies SET
    description = 'After Thanos devastates the universe, the remaining Avengers make one final stand to reverse the loss.'
WHERE id = 1 AND description IS NULL
  AND director = 'Anthony Russo' AND actors = 'Robert Downey Jr., Chris Evans'
  AND release_year = 2019 AND rating = 8.4;

UPDATE movies SET
    description = 'The Avengers and their allies unite against Thanos as he seeks the Infinity Stones.'
WHERE id = 2 AND description IS NULL
  AND director = 'Anthony Russo' AND actors = 'Robert Downey Jr., Chris Hemsworth'
  AND release_year = 2018 AND rating = 8.4;

UPDATE movies SET
    description = 'When Peter Parker asks Doctor Strange to restore his secret identity, the spell releases dangerous villains from across the multiverse.',
    actors = 'Tom Holland, Zendaya, Benedict Cumberbatch, Jacob Batalon, Jon Favreau'
WHERE id = 3 AND description IS NULL
  AND director = 'Jon Watts' AND actors = 'Tom Holland, Zendaya'
  AND release_year = 2021 AND rating = 8.2;

UPDATE movies SET
    description = 'A former Marine joins a mission to Pandora and, after meeting the Na''vi, is drawn into a conflict that will decide the moon''s future.',
    actors = 'Sam Worthington, Zoe Saldaña, Stephen Lang, Michelle Rodriguez, Sigourney Weaver'
WHERE id = 41 AND description IS NULL
  AND director = 'James Cameron' AND actors = 'Sam Worthington, Zoe Saldana'
  AND release_year = 2009 AND rating = 7.9;

UPDATE movies SET
    description = 'Teenage Riley faces new emotions when Anxiety arrives at Headquarters during a turbulent transition.',
    actors = 'Maya Hawke, Amy Poehler, Phyllis Smith, Lewis Black, Tony Hale, Liza Lapira'
WHERE id = 53 AND description IS NULL
  AND director = 'Kelsey Mann' AND actors = 'Amy Poehler, Maya Hawke'
  AND release_year = 2024 AND rating = 8.8;
