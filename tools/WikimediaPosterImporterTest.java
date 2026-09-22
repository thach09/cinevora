import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Focused offline tests for the Wikimedia poster importer. */
public final class WikimediaPosterImporterTest {
    private static int passed;

    private WikimediaPosterImporterTest() {
    }

    public static void main(String[] args) {
        testExactHighConfidenceMatch();
        testYearMismatchRequiresReview();
        testNonPortraitImageIsRejected();
        testMissingPageIsNotFound();
        testPunctuationNormalization();
        testProviderTitleVariants();
        testLocalizedTitleAlias();
        testOfficialPosterSources();
        testSearchFallbackExactMatch();
        testWikipediaFilmTitleWithoutYearSuffix();
        testDryRunDoesNotUpdate();
        testApplyPreservesAllNonPosterFields();
        testExistingPosterIsSkipped();
        testForceRechecksExistingPoster();
        testApiUrlNormalization();
        System.out.println("WikimediaPosterImporterTest: " + passed + " assertions passed");
    }

    private static void testExactHighConfidenceMatch() {
        WikimediaPosterImporter.Movie movie = movie("Dune", 2021, null);
        WikimediaPosterImporter.PosterCandidate candidate = candidate("Dune", 2021,
                "https://en.wikipedia.org/wiki/Dune", "https://upload.wikimedia.org/dune.jpg",
                500, 750);
        WikimediaPosterImporter.LookupResult result = WikimediaPosterImporter.resolveDirect(
                movie, candidate, "standard", "2021 film directed by Denis Villeneuve");
        check(result.status() == WikimediaPosterImporter.Status.MATCHED_HIGH_CONFIDENCE,
                "exact title/year/film evidence should match");
    }

    private static void testYearMismatchRequiresReview() {
        WikimediaPosterImporter.Movie movie = movie("Dune", 2021, null);
        WikimediaPosterImporter.LookupResult result = WikimediaPosterImporter.resolveDirect(
                movie, candidate("Dune", 2022, "https://en.wikipedia.org/wiki/Dune", "https://upload.wikimedia.org/dune.jpg", 500, 750),
                "standard", "2022 film directed by Denis Villeneuve");
        check(result.status() == WikimediaPosterImporter.Status.REVIEW_REQUIRED,
                "year mismatch should never auto-apply");
    }

    private static void testNonPortraitImageIsRejected() {
        WikimediaPosterImporter.Movie movie = movie("Dune", 2021, null);
        WikimediaPosterImporter.LookupResult result = WikimediaPosterImporter.resolveDirect(
                movie, candidate("Dune", 2021, "https://en.wikipedia.org/wiki/Dune", "https://upload.wikimedia.org/dune.jpg", 1200, 700),
                "standard", "2021 film directed by Denis Villeneuve");
        check(result.status() == WikimediaPosterImporter.Status.NO_POSTER,
                "landscape image should not be used as a poster");
    }

    private static void testMissingPageIsNotFound() {
        WikimediaPosterImporter.LookupResult result = WikimediaPosterImporter.resolveDirect(
                movie("Missing Movie", 2021, null), null, "standard", "");
        check(result.status() == WikimediaPosterImporter.Status.NOT_FOUND,
                "missing exact page should be reported as not found");
    }

    private static void testPunctuationNormalization() {
        check(WikimediaPosterImporter.titleMatches("Spider-Man: No Way Home", "Spider Man No Way Home"),
                "title normalization should ignore punctuation differences");
    }

    private static void testProviderTitleVariants() {
        check(WikimediaPosterImporter.titleVariantMatches("Annabelle", "Annabelle (2014 film)"),
                "provider year suffix should be accepted for an exact film title");
        check(WikimediaPosterImporter.titleVariantMatches("Dune: Part One", "Dune (2021 film)"),
                "Dune Part One should accept the canonical film page title");
    }

    private static void testLocalizedTitleAlias() {
        check(WikimediaPosterImporter.titleVariantMatches("Mắt Biếc", "Dreamy Eyes (film)"),
                "localized English film title should match Mắt Biếc");
    }

    private static void testOfficialPosterSources() {
        WikimediaPosterImporter.PosterCandidate candidate = WikimediaPosterImporter.officialPoster(
                movie("Bóng Đè", 2022, null));
        check(candidate != null && WikimediaPosterImporter.isPortraitPoster(candidate)
                        && WikimediaPosterImporter.isValidPosterUrl(candidate.posterUrl()),
                "official rightsholder poster source should validate");
    }

    private static void testSearchFallbackExactMatch() {
        WikimediaPosterImporter.Movie movie = movie("Annabelle", 2014, null);
        WikimediaPosterImporter.PosterCandidate candidate = candidate("Annabelle (2014 film)", 2014,
                "https://en.wikipedia.org/wiki/Annabelle_(film)", "https://upload.wikimedia.org/annabelle.jpg",
                500, 750);
        WikimediaPosterImporter.LookupResult result = WikimediaPosterImporter.resolveDirect(
                movie, candidate, "standard", "2014 film");
        check(result.status() == WikimediaPosterImporter.Status.MATCHED_HIGH_CONFIDENCE,
                "exact fallback title/year/film evidence should match");
    }

    private static void testWikipediaFilmTitleWithoutYearSuffix() {
        WikimediaPosterImporter.Movie movie = movie("Annabelle", 2014, null);
        WikimediaPosterImporter.PosterCandidate candidate = candidate("Annabelle (film)", 2014,
                "https://en.wikipedia.org/wiki/Annabelle_(film)",
                "https://upload.wikimedia.org/wikipedia/en/9/90/Annabelle_film_poster.jpg?utm_source=test",
                220, 326);
        WikimediaPosterImporter.LookupResult result = WikimediaPosterImporter.resolveDirect(
                movie, candidate, "standard", "2014 film directed by John R. Leonetti");
        check(result.status() == WikimediaPosterImporter.Status.MATCHED_HIGH_CONFIDENCE,
                "Wikipedia film title without a year suffix should match");
    }

    private static void testDryRunDoesNotUpdate() {
        WikimediaPosterImporter.Movie movie = movie("Dune", 2021, null);
        FakeProvider provider = new FakeProvider(high(movie));
        FakeCinevora cinevora = new FakeCinevora();
        List<WikimediaPosterImporter.ImportResult> results = WikimediaPosterImporter.runImport(
                List.of(movie), provider, cinevora, "token", new WikimediaPosterImporter.Options(false, false, null, null));
        check(results.get(0).status() == WikimediaPosterImporter.Status.MATCHED_HIGH_CONFIDENCE,
                "dry-run should still resolve a high-confidence match");
        check(cinevora.updateCount == 0, "dry-run must not issue update request");
    }

    private static void testApplyPreservesAllNonPosterFields() {
        WikimediaPosterImporter.Movie movie = movie("Dune", 2021, null);
        FakeProvider provider = new FakeProvider(high(movie));
        FakeCinevora cinevora = new FakeCinevora();
        List<WikimediaPosterImporter.ImportResult> results = WikimediaPosterImporter.runImport(
                List.of(movie), provider, cinevora, "token", new WikimediaPosterImporter.Options(true, false, null, null));
        Map<String, Object> payload = cinevora.lastPayload;
        check(results.get(0).applied(), "apply mode should mark the update as applied");
        check(cinevora.updateCount == 1, "apply mode should issue one update request");
        check("https://cdn.example/video.mp4".equals(payload.get("videoUrl")), "videoUrl must be preserved");
        check("https://cdn.example/trailer.mp4".equals(payload.get("trailerUrl")), "trailerUrl must be preserved");
        check(movie.categoryId().equals(payload.get("categoryId")), "categoryId must be preserved");
        check("https://upload.wikimedia.org/dune.jpg".equals(payload.get("thumbnailUrl")),
                "only thumbnailUrl should receive the new poster URL");
    }

    private static void testExistingPosterIsSkipped() {
        WikimediaPosterImporter.Movie movie = movie("Dune", 2021, "https://existing.example/dune.jpg");
        FakeProvider provider = new FakeProvider(high(movie));
        FakeCinevora cinevora = new FakeCinevora();
        List<WikimediaPosterImporter.ImportResult> results = WikimediaPosterImporter.runImport(
                List.of(movie), provider, cinevora, "token", new WikimediaPosterImporter.Options(false, false, null, null));
        check(results.get(0).status() == WikimediaPosterImporter.Status.ALREADY_HAS_POSTER,
                "existing poster should be skipped by default");
        check(provider.calls == 0, "skipped movie should not call Wikimedia");
    }

    private static void testForceRechecksExistingPoster() {
        WikimediaPosterImporter.Movie movie = movie("Dune", 2021, "https://existing.example/dune.jpg");
        FakeProvider provider = new FakeProvider(high(movie));
        FakeCinevora cinevora = new FakeCinevora();
        WikimediaPosterImporter.runImport(List.of(movie), provider, cinevora, "token",
                new WikimediaPosterImporter.Options(false, true, null, null));
        check(provider.calls == 1, "force mode should recheck an existing poster");
    }

    private static void testApiUrlNormalization() {
        check("http://localhost:8080/api/v1".equals(
                        WikimediaPosterImporter.normalizeCinevoraApiUrl("http://localhost:8080/")),
                "root Cinevora URL should normalize to /api/v1");
        check("http://localhost:8080/api/v1".equals(
                        WikimediaPosterImporter.normalizeCinevoraApiUrl("http://localhost:8080/api/v1")),
                "versioned Cinevora URL should not duplicate /api/v1");
    }

    private static WikimediaPosterImporter.LookupResult high(WikimediaPosterImporter.Movie movie) {
        return WikimediaPosterImporter.LookupResult.highConfidence(candidate(movie.title(), movie.releaseYear(),
                "https://en.wikipedia.org/wiki/Dune", "https://upload.wikimedia.org/dune.jpg", 500, 750));
    }

    private static WikimediaPosterImporter.PosterCandidate candidate(String title, Integer year, String pageUrl,
                                                                       String posterUrl, int width, int height) {
        return new WikimediaPosterImporter.PosterCandidate(title, year, pageUrl, posterUrl, "en",
                year + " film", width, height, false);
    }

    private static WikimediaPosterImporter.Movie movie(String title, int year, String thumbnailUrl) {
        return new WikimediaPosterImporter.Movie(7L, 2L, title, "Director", "Actor", year,
                new BigDecimal("8.2"), 155, "https://cdn.example/video.mp4", "https://cdn.example/trailer.mp4", thumbnailUrl,
                "Description", true);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        passed++;
    }

    private static final class FakeProvider implements WikimediaPosterImporter.PosterProvider {
        private final WikimediaPosterImporter.LookupResult result;
        private int calls;

        private FakeProvider(WikimediaPosterImporter.LookupResult result) {
            this.result = result;
        }

        @Override
        public WikimediaPosterImporter.LookupResult lookup(WikimediaPosterImporter.Movie movie) {
            calls++;
            return result;
        }
    }

    private static final class FakeCinevora implements WikimediaPosterImporter.CinevoraGateway {
        private int updateCount;
        private Map<String, Object> lastPayload = Map.of();

        @Override
        public void updatePoster(WikimediaPosterImporter.Movie movie, String posterUrl, String token) {
            updateCount++;
            lastPayload = movie.updatePayload(posterUrl);
        }

        @Override
        public List<WikimediaPosterImporter.Movie> loadActiveMovies(String token) {
            return new ArrayList<>();
        }
    }
}
