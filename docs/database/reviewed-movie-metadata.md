# Reviewed movie metadata

V11 is an initial, conservative enrichment cohort. It does not alter V1/V2 and
only changes a movie when its V2 fingerprint still matches and its synopsis is
empty. Any admin edit to the protected fields makes the update a no-op.

| Seed ID | Title | Verified source | Imported fields |
| --- | --- | --- | --- |
| 1 | Avengers: Endgame | [Marvel Studios](https://www.marvel.com/movies/avengers-untitled) | Synopsis |
| 2 | Avengers: Infinity War | [Marvel Studios](https://www.marvel.com/movies/avengers-infinity-war) | Synopsis |
| 3 | Spider-Man: No Way Home | [Sony Pictures](https://ceema.sonypictures.com/en/movies/spider-man-no-way-home) | Synopsis, principal cast |
| 41 | Avatar | [20th Century Studios](https://www.20thcenturystudios.com/movies/avatar) | Synopsis, principal cast |
| 53 | Inside Out 2 | [Disney](https://movies.disney.com/inside-out-2) | Synopsis, principal cast |

Descriptions are concise original summaries of the cited official synopsis,
not copied marketing text. Titles without a reviewed official source remain
unchanged until they are reviewed in a later additive migration.
