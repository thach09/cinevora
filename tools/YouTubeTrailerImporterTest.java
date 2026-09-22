import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Focused offline tests for the legal trailer importer. */
public final class YouTubeTrailerImporterTest {
    private static int passed;

    private YouTubeTrailerImporterTest() {
    }

    public static void main(String[] args) {
        testYouTubeUrlValidation();
        testTitleNormalization();
        testDryRunDoesNotUpdate();
        testApplyPreservesAllNonTrailerFields();
        testExistingTrailerIsSkipped();
        System.out.println("YouTubeTrailerImporterTest: " + passed + " assertions passed");
    }

    private static void testYouTubeUrlValidation() {
        check(YouTubeTrailerImporter.isValidYouTubeUrl("https://youtu.be/JfVOs4VSpmA?si=demo"),
                "short YouTube URL should validate");
        check(YouTubeTrailerImporter.isValidYouTubeUrl("https://www.youtube.com/watch?v=JfVOs4VSpmA"),
                "watch YouTube URL should validate");
        check(!YouTubeTrailerImporter.isValidYouTubeUrl("https://example.com/trailer.mp4"),
                "non-YouTube URL should not be classified as YouTube");
    }

    private static void testTitleNormalization() {
        check(YouTubeTrailerImporter.normalizeTitle("Spider-Man: No Way Home")
                        .equals(YouTubeTrailerImporter.normalizeTitle("Spider Man No Way Home")),
                "title normalization should ignore punctuation differences");
    }

    private static void testDryRunDoesNotUpdate() {
        FakeCinevora cinevora = new FakeCinevora();
        List<YouTubeTrailerImporter.ImportResult> results = YouTubeTrailerImporter.runImport(
                List.of(movie(null)), provider(), cinevora, "token",
                new YouTubeTrailerImporter.Options(false, false, null, null));
        check(results.get(0).status() == YouTubeTrailerImporter.Status.MATCHED_CURATED_SOURCE,
                "dry-run should resolve curated source");
        check(cinevora.updateCount == 0, "dry-run must not issue update request");
    }

    private static void testApplyPreservesAllNonTrailerFields() {
        FakeCinevora cinevora = new FakeCinevora();
        List<YouTubeTrailerImporter.ImportResult> results = YouTubeTrailerImporter.runImport(
                List.of(movie(null)), provider(), cinevora, "token",
                new YouTubeTrailerImporter.Options(true, false, null, null));
        check(results.get(0).applied(), "apply mode should mark update as applied");
        check("https://upload.wikimedia.org/poster.jpg".equals(cinevora.lastPayload.get("thumbnailUrl")),
                "thumbnailUrl must be preserved");
        check("https://www.youtube.com/watch?v=JfVOs4VSpmA".equals(cinevora.lastPayload.get("trailerUrl")),
                "trailerUrl should receive the curated trailer");
        check(cinevora.lastPayload.get("videoUrl") == null,
                "Watch Now videoUrl must remain separate");
        check(Long.valueOf(2).equals(cinevora.lastPayload.get("categoryId")),
                "categoryId must be preserved");
    }

    private static void testExistingTrailerIsSkipped() {
        FakeCinevora cinevora = new FakeCinevora();
        List<YouTubeTrailerImporter.ImportResult> results = YouTubeTrailerImporter.runImport(
                List.of(movie(null, "https://existing.example/trailer.mp4")), provider(), cinevora, "token",
                new YouTubeTrailerImporter.Options(false, false, null, null));
        check(results.get(0).status() == YouTubeTrailerImporter.Status.ALREADY_HAS_TRAILER,
                "existing trailerUrl should be skipped by default");
        check(cinevora.updateCount == 0, "existing trailerUrl should not be overwritten by default");
    }

    private static YouTubeTrailerImporter.TrailerProvider provider() {
        return movie -> YouTubeTrailerImporter.LookupResult.matched(new YouTubeTrailerImporter.TrailerCandidate(
                movie.title(), "https://www.youtube.com/watch?v=JfVOs4VSpmA", "Sony Pictures Entertainment",
                "https://www.youtube.com/watch?v=JfVOs4VSpmA", false));
    }

    private static YouTubeTrailerImporter.Movie movie(String videoUrl) {
        return movie(videoUrl, null);
    }

    private static YouTubeTrailerImporter.Movie movie(String videoUrl, String trailerUrl) {
        return new YouTubeTrailerImporter.Movie(7L, 2L, "Spider-Man: No Way Home", "Director", "Actor", 2021,
                new BigDecimal("8.2"), 148, videoUrl, trailerUrl,
                "https://upload.wikimedia.org/poster.jpg", "Description", true);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        passed++;
    }

    private static final class FakeCinevora implements YouTubeTrailerImporter.CinevoraGateway {
        private int updateCount;
        private Map<String, Object> lastPayload = Map.of();

        @Override
        public void updateTrailer(YouTubeTrailerImporter.Movie movie, String trailerUrl, String token) {
            updateCount++;
            lastPayload = movie.updatePayload(trailerUrl);
        }

        @Override
        public List<YouTubeTrailerImporter.Movie> loadActiveMovies(String token) {
            return new ArrayList<>();
        }
    }
}
