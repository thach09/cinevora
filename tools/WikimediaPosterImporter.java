import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One-shot Wikimedia poster enrichment utility for Cinevora.
 *
 * <p>The default mode is dry-run. Wikimedia is queried only by this developer
 * utility; normal Cinevora browsing remains React -> Spring Boot -> PostgreSQL.
 * The utility never writes directly to PostgreSQL.</p>
 */
public final class WikimediaPosterImporter {
    private static final String DEFAULT_CINEVORA_API_URL = "http://localhost:8080/api/v1";
    private static final Path DEFAULT_REPORT = Path.of("docs", "verification", "POSTER_IMPORT_REPORT.md");
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration WIKIMEDIA_REQUEST_TIMEOUT = Duration.ofSeconds(8);
    private static final int MAX_ATTEMPTS = 2;
    private static final long WIKIMEDIA_MIN_INTERVAL_MILLIS = 1_100L;
    private static final Pattern YEAR_PATTERN = Pattern.compile("(?<!\\d)(18|19|20|21)\\d{2}(?!\\d)");
    private static final String USER_AGENT = "CinevoraPosterImporter/1.0 (developer metadata enrichment)";

    private WikimediaPosterImporter() {
    }

    public static void main(String[] args) {
        try {
            Options options = Options.parse(args);
            Config config = Config.fromEnvironment();
            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(HTTP_TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpCinevoraGateway cinevora = new HttpCinevoraGateway(http, config.cinevoraApiUrl(),
                    config.adminUsername(), config.adminPassword());
            WikimediaGateway wikimedia = new WikimediaGateway(http);

            String token = cinevora.login();
            List<Movie> movies = cinevora.loadActiveMovies(token);
            if (options.movieId() != null) {
                movies = movies.stream().filter(movie -> movie.id().equals(options.movieId())).toList();
            }
            if (options.limit() != null && movies.size() > options.limit()) {
                movies = movies.subList(0, options.limit());
            }

            System.out.println("Wikimedia poster import mode: " + (options.apply() ? "APPLY" : "DRY-RUN"));
            System.out.println("Movies loaded from Cinevora: " + movies.size());
            List<ImportResult> results = runImport(movies, wikimedia, cinevora, token, options);
            if (options.apply()) {
                verifyPersistedPosters(cinevora, token, results);
            }

            String report = Report.render(results, options, config, readGitCommit());
            Path reportParent = config.reportPath().getParent();
            if (reportParent != null) Files.createDirectories(reportParent);
            Files.writeString(config.reportPath(), report, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            printConsoleReport(results, config.reportPath());
        } catch (ConfigurationException e) {
            System.err.println(e.getMessage());
            System.exit(2);
        } catch (Exception e) {
            System.err.println("Wikimedia poster import failed: " + safeMessage(e));
            System.exit(1);
        }
    }

    public static List<ImportResult> runImport(List<Movie> movies, PosterProvider provider,
                                               CinevoraGateway cinevora, String token, Options options) {
        List<ImportResult> results = new ArrayList<>();
        String providerFailureReason = null;
        for (Movie movie : movies) {
            if (!options.force() && hasPoster(movie.thumbnailUrl())) {
                results.add(ImportResult.skipped(movie));
                continue;
            }
            if (providerFailureReason != null) {
                results.add(ImportResult.error(movie, providerFailureReason));
                continue;
            }
            try {
                LookupResult lookup = provider.lookup(movie);
                ImportResult result = ImportResult.fromLookup(movie, lookup);
                if (options.apply() && result.status() == Status.MATCHED_HIGH_CONFIDENCE) {
                    cinevora.updatePoster(movie, result.posterUrl(), token);
                    result = result.withApplied(true);
                }
                results.add(result);
            } catch (ProviderFailure e) {
                providerFailureReason = e.getMessage() == null ? "PROVIDER_ERROR" : e.getMessage();
                results.add(ImportResult.error(movie, providerFailureReason));
            } catch (Exception e) {
                results.add(ImportResult.error(movie, safeCategory(e)));
            }
        }
        return List.copyOf(results);
    }

    public interface PosterProvider {
        LookupResult lookup(Movie movie) throws Exception;
    }

    public interface CinevoraGateway {
        void updatePoster(Movie movie, String posterUrl, String token) throws Exception;

        List<Movie> loadActiveMovies(String token) throws Exception;
    }

    public record Movie(Long id, Long categoryId, String title, String director, String actors,
                        Integer releaseYear, BigDecimal rating, Integer durationMinutes,
                        String videoUrl, String trailerUrl, String thumbnailUrl, String description, boolean active) {
        public Map<String, Object> updatePayload(String posterUrl) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("title", title);
            payload.put("categoryId", categoryId);
            payload.put("director", director);
            payload.put("actors", actors);
            payload.put("releaseYear", releaseYear);
            payload.put("rating", rating);
            payload.put("durationMinutes", durationMinutes);
            payload.put("videoUrl", videoUrl);
            payload.put("trailerUrl", trailerUrl);
            payload.put("thumbnailUrl", posterUrl);
            payload.put("description", description);
            return payload;
        }
    }

    public record PosterCandidate(String title, Integer year, String pageUrl, String posterUrl,
                                  String language, String description, int width, int height,
                                  boolean fallback) {
    }

    public enum Status {
        ALREADY_HAS_POSTER,
        MATCHED_HIGH_CONFIDENCE,
        REVIEW_REQUIRED,
        AMBIGUOUS,
        NOT_FOUND,
        NO_POSTER,
        ERROR
    }

    public record LookupResult(Status status, PosterCandidate candidate, String reason) {
        public static LookupResult highConfidence(PosterCandidate candidate) {
            return new LookupResult(Status.MATCHED_HIGH_CONFIDENCE, candidate, "EXACT_PAGE_TITLE_YEAR_FILM");
        }

        public static LookupResult review(PosterCandidate candidate, String reason) {
            return new LookupResult(Status.REVIEW_REQUIRED, candidate, reason);
        }

        public static LookupResult ambiguous(PosterCandidate candidate, String reason) {
            return new LookupResult(Status.AMBIGUOUS, candidate, reason);
        }

        public static LookupResult noPoster(PosterCandidate candidate) {
            return new LookupResult(Status.NO_POSTER, candidate, "POSTER_MISSING_OR_NOT_PORTRAIT");
        }

        public static LookupResult notFound() {
            return new LookupResult(Status.NOT_FOUND, null, "WIKIMEDIA_NOT_FOUND");
        }
    }

    public static final class ImportResult {
        private final Movie movie;
        private final Status status;
        private final PosterCandidate candidate;
        private final String reason;
        private final String posterUrl;
        private final boolean applied;
        private volatile Boolean verified;
        private volatile String verificationError;

        private ImportResult(Movie movie, Status status, PosterCandidate candidate, String reason,
                             String posterUrl, boolean applied) {
            this.movie = movie;
            this.status = status;
            this.candidate = candidate;
            this.reason = reason;
            this.posterUrl = posterUrl;
            this.applied = applied;
        }

        public static ImportResult skipped(Movie movie) {
            return new ImportResult(movie, Status.ALREADY_HAS_POSTER, null, "EXISTING_POSTER", null, false);
        }

        public static ImportResult fromLookup(Movie movie, LookupResult lookup) {
            String poster = lookup.candidate() == null ? null : lookup.candidate().posterUrl();
            return new ImportResult(movie, lookup.status(), lookup.candidate(), lookup.reason(), poster, false);
        }

        public static ImportResult error(Movie movie, String reason) {
            return new ImportResult(movie, Status.ERROR, null, reason, null, false);
        }

        public ImportResult withApplied(boolean applied) {
            return new ImportResult(movie, status, candidate, reason, posterUrl, applied);
        }

        public Movie movie() { return movie; }
        public Status status() { return status; }
        public PosterCandidate candidate() { return candidate; }
        public String reason() { return reason; }
        public String posterUrl() { return posterUrl; }
        public boolean applied() { return applied; }
        public Boolean verified() { return verified; }
        public String verificationError() { return verificationError; }
        private void setVerified(boolean value) { verified = value; }
        private void setVerificationError(String value) { verificationError = value; }
    }

    public record Options(boolean apply, boolean force, Long movieId, Integer limit) {
        public static Options parse(String[] args) {
            boolean apply = false;
            boolean dryRun = false;
            boolean force = false;
            Long movieId = null;
            Integer limit = null;
            for (String arg : args) {
                if ("--apply".equals(arg)) apply = true;
                else if ("--dry-run".equals(arg)) dryRun = true;
                else if ("--force".equals(arg)) force = true;
                else if (arg.startsWith("--movie-id=")) movieId = positiveLong(arg.substring(11), "movie-id");
                else if (arg.startsWith("--limit=")) limit = positiveInt(arg.substring(8), "limit");
                else if (!arg.isBlank()) throw new ConfigurationException("Unknown option: " + arg);
            }
            if (apply && dryRun) throw new ConfigurationException("Use either --dry-run or --apply, not both");
            return new Options(apply, force, movieId, limit);
        }
    }

    private record Config(String cinevoraApiUrl, String adminUsername, String adminPassword, Path reportPath) {
        static Config fromEnvironment() {
            return new Config(
                    normalizeCinevoraApiUrl(optionalEnv("CINEVORA_API_URL", DEFAULT_CINEVORA_API_URL)),
                    requiredEnv("CINEVORA_ADMIN_USERNAME"),
                    requiredEnv("CINEVORA_ADMIN_PASSWORD"),
                    Path.of(optionalEnv("POSTER_IMPORT_REPORT_PATH", DEFAULT_REPORT.toString())));
        }
    }

    static String normalizeCinevoraApiUrl(String value) {
        String result = trimTrailingSlash(value);
        return result.endsWith("/api/v1") ? result : result + "/api/v1";
    }

    public static String normalizeTitle(String value) {
        if (value == null) return "";
        String lower = value.trim().toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();
        for (int offset = 0; offset < lower.length();) {
            int codePoint = lower.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.isLetterOrDigit(codePoint)) {
                result.appendCodePoint(codePoint);
            } else if (!result.isEmpty()) {
                result.append(' ');
            }
        }
        return result.toString().replaceAll("\\s+", " ").trim();
    }

    public static boolean titleMatches(String left, String right) {
        return !normalizeTitle(left).isEmpty() && normalizeTitle(left).equals(normalizeTitle(right));
    }

    static boolean titleVariantMatches(String movieTitle, String providerTitle) {
        String movie = normalizeTitle(movieTitle);
        String provider = normalizeTitle(providerTitle);
        if (movie.isEmpty() || provider.isEmpty()) return false;
        if (movie.equals(provider)) return true;
        String providerBase = provider.replaceAll("\\s+(?:18|19|20|21)[0-9]{2}\\s+film$", "")
                .replaceAll("\\s+film$", "").trim();
        if (movie.equals(providerBase)) return true;
        if (movie.replace(" chapter ", " ").equals(provider.replace(" chapter ", " "))) return true;
        if (movie.equals(normalizeTitle("Mắt Biếc"))
                && provider.equals(normalizeTitle("Dreamy Eyes (film)"))) return true;
        return movie.equals("dune part one") && providerBase.equals("dune");
    }

    static PosterCandidate officialPoster(Movie movie) {
        String title = normalizeTitle(movie.title());
        if (title.equals(normalizeTitle("Bóng Đè")) && Objects.equals(movie.releaseYear(), 2022)) {
            return new PosterCandidate(movie.title(), 2022, "https://rubikpictures.com/en/bong-de/",
                    "https://rubikpictures.com/wp-content/uploads/2023/06/bong-de.jpg", "official",
                    "2022 Vietnamese horror film; official production portfolio", 600, 900, false);
        }
        if (title.equals(normalizeTitle("Chàng Vợ Của Em")) && Objects.equals(movie.releaseYear(), 2018)) {
            return new PosterCandidate(movie.title(), 2018,
                    "https://sovhtt.hanoi.gov.vn/goc-khuat-thu-vi-cua-doi-song-hien-dai-trong-chang-vo-cua-em/",
                    "https://sovhtt.hanoi.gov.vn/wp-content/uploads/2018/05/CH%C3%80NG-V%E1%BB%A2-C%E1%BB%A6A-EM_-TEASER-POSTER-405x600.jpg",
                    "official", "2018 Vietnamese film; poster reproduced by a government cultural portal",
                    405, 600, false);
        }
        if (title.equals(normalizeTitle("Maika: Cô Bé Đến Từ Hành Tinh Khác"))
                && Objects.equals(movie.releaseYear(), 2022)) {
            return new PosterCandidate(movie.title(), 2022,
                    "https://tv.apple.com/ca/movie/maika/umc.cmc.3rrtooc32h0d5eiv97yhcc3as",
                    "https://is1-ssl.mzstatic.com/image/thumb/Video112/v4/fc/f7/61/fcf76155-7a6d-c143-6d46-acf8154ce38b/maika_poster_p3.lsr/1200x2133.webp",
                    "official", "2022 Vietnamese film; Apple TV rights listing", 1200, 2133, false);
        }
        if (title.equals(normalizeTitle("Mr. Bean's Holiday")) && Objects.equals(movie.releaseYear(), 2007)) {
            return new PosterCandidate(movie.title(), 2007,
                    "https://www.universalpicturesathome.com/movies/mr-beans-holiday",
                    "https://images.contentstack.io/v3/assets/blt13adb7e2033fcee5/blt08ffc31f7b1f3e52/691c88d3f1c203a10d0b69db/MrBeansHoliday_Poster_2000x3000_uaa.jpg?width=1200",
                    "official", "2007 film; Universal Pictures At Home poster asset", 1200, 1800, false);
        }
        return null;
    }

    public static boolean isValidPosterUrl(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            URI uri = URI.create(value.trim());
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isPortraitPoster(PosterCandidate candidate) {
        return candidate != null && candidate.width() > 0 && candidate.height() > 0
                && candidate.height() >= Math.round(candidate.width() * 1.10f);
    }

    public static LookupResult resolveDirect(Movie movie, PosterCandidate candidate, String pageType,
                                              String text) {
        if (candidate == null) return LookupResult.notFound();
        if (!titleVariantMatches(movie.title(), candidate.title())) {
            return LookupResult.ambiguous(candidate, "PAGE_TITLE_NOT_EXACT");
        }
        if ("disambiguation".equalsIgnoreCase(pageType)) {
            return LookupResult.ambiguous(candidate, "DISAMBIGUATION_PAGE");
        }
        boolean yearMatches = containsYear(text, movie.releaseYear());
        boolean filmEvidence = hasFilmEvidence(text);
        if (!yearMatches || !filmEvidence) {
            return LookupResult.review(candidate, "PAGE_NEEDS_YEAR_OR_FILM_REVIEW");
        }
        if (!isValidPosterUrl(candidate.posterUrl()) || !isPortraitPoster(candidate)) {
            return LookupResult.noPoster(candidate);
        }
        return LookupResult.highConfidence(candidate);
    }

    static boolean containsYear(String text, Integer year) {
        if (text == null || year == null) return false;
        Matcher matcher = YEAR_PATTERN.matcher(text);
        while (matcher.find()) {
            if (Integer.parseInt(matcher.group()) == year) return true;
        }
        return false;
    }

    static boolean hasFilmEvidence(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("film") || lower.contains("movie") || lower.contains("phim")
                || lower.contains("điện ảnh");
    }

    private static void verifyPersistedPosters(CinevoraGateway cinevora, String token,
                                                List<ImportResult> results) {
        Map<Long, String> expected = new HashMap<>();
        for (ImportResult result : results) {
            if (result.applied() && result.posterUrl() != null) expected.put(result.movie().id(), result.posterUrl());
        }
        if (expected.isEmpty()) return;
        try {
            Map<Long, Movie> reloaded = new HashMap<>();
            for (Movie movie : cinevora.loadActiveMovies(token)) reloaded.put(movie.id(), movie);
            for (ImportResult result : results) {
                if (result.applied()) {
                    Movie reloadedMovie = reloaded.get(result.movie().id());
                    result.setVerified(reloadedMovie != null && Objects.equals(
                            reloadedMovie.thumbnailUrl(), expected.get(result.movie().id())));
                }
            }
        } catch (Exception e) {
            for (ImportResult result : results) if (result.applied()) result.setVerificationError(safeCategory(e));
        }
    }

    private static final class HttpCinevoraGateway implements CinevoraGateway {
        private final HttpClient http;
        private final String baseUrl;
        private final String username;
        private final String password;

        private HttpCinevoraGateway(HttpClient http, String baseUrl, String username, String password) {
            this.http = http; this.baseUrl = baseUrl; this.username = username; this.password = password;
        }

        private String login() throws Exception {
            HttpResponse<String> response = request("POST", baseUrl + "/auth/login", null,
                    Json.stringify(Map.of("username", username, "password", password)));
            requireSuccess(response, "Cinevora login");
            Map<String, Object> root = Json.object(Json.parse(response.body()));
            String token = Json.string(Json.object(root.get("data")), "token");
            if (token == null || token.isBlank()) throw new ApiFailure("Cinevora login returned no token");
            return token;
        }

        @Override
        public List<Movie> loadActiveMovies(String token) throws Exception {
            List<Movie> movies = new ArrayList<>();
            int page = 0;
            int totalPages;
            do {
                String query = "?includeInactive=false&page=" + page + "&size=50&sort=title&direction=asc";
                HttpResponse<String> response = request("GET", baseUrl + "/admin/movies" + query, token, null);
                requireSuccess(response, "Cinevora movie page " + page);
                Map<String, Object> data = Json.object(Json.object(Json.parse(response.body())).get("data"));
                for (Object item : Json.array(data.get("content"))) movies.add(parseMovie(Json.object(item)));
                totalPages = Json.integer(data.get("totalPages"), page + 1);
                page++;
                if (page > 10000) throw new ApiFailure("Cinevora pagination exceeded safety limit");
            } while (page < totalPages);
            return List.copyOf(movies);
        }

        @Override
        public void updatePoster(Movie movie, String posterUrl, String token) throws Exception {
            if (!isValidPosterUrl(posterUrl)) throw new ApiFailure("Invalid poster URL for movie " + movie.id());
            HttpResponse<String> response = request("PUT", baseUrl + "/movies/" + movie.id(), token,
                    Json.stringify(movie.updatePayload(posterUrl)));
            requireSuccess(response, "Cinevora update movie " + movie.id());
        }

        private HttpResponse<String> request(String method, String url, String token, String body) throws Exception {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(HTTP_TIMEOUT)
                    .header("Accept", "application/json");
            if (token != null) builder.header("Authorization", "Bearer " + token);
            if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
            else builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            return sendWithRetries(http, builder.build(), false);
        }

        private static Movie parseMovie(Map<String, Object> object) {
            return new Movie(Json.longValue(object.get("id")), Json.longValue(object.get("categoryId")),
                    Json.string(object, "title"), Json.string(object, "director"), Json.string(object, "actors"),
                    Json.integer(object.get("releaseYear"), null), Json.decimal(object.get("rating")),
                    Json.integer(object.get("durationMinutes"), null), Json.string(object, "videoUrl"),
                    Json.string(object, "trailerUrl"),
                    Json.string(object, "thumbnailUrl"), Json.string(object, "description"), Json.bool(object.get("active")));
        }

        private static void requireSuccess(HttpResponse<String> response, String operation) throws ApiFailure {
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiFailure(operation + " returned HTTP " + response.statusCode());
            }
            Map<String, Object> root;
            try { root = Json.object(Json.parse(response.body())); }
            catch (RuntimeException e) { throw new ApiFailure(operation + " returned invalid JSON"); }
            if (!Json.bool(root.get("success"))) throw new ApiFailure(operation + " returned success=false");
        }
    }

    static final class WikimediaGateway implements PosterProvider {
        private final HttpClient http;
        private long lastRequestMillis;

        WikimediaGateway(HttpClient http) { this.http = http; }

        @Override
        public LookupResult lookup(Movie movie) throws Exception {
            PosterCandidate official = officialPoster(movie);
            if (official != null && isValidPosterUrl(official.posterUrl()) && isPortraitPoster(official)) {
                return new LookupResult(Status.MATCHED_HIGH_CONFIDENCE, official, "OFFICIAL_RIGHTSHOLDER_POSTER");
            }
            LookupResult best = null;
            if (normalizeTitle(movie.title()).equals(normalizeTitle("Mắt Biếc"))) {
                WikiPage localizedPage = fetchSummary("en", "Dreamy Eyes (film)");
                if (localizedPage != null) {
                    PosterCandidate localizedCandidate = localizedPage.candidate("en", false);
                    LookupResult localized = resolveDirect(movie, localizedCandidate, localizedPage.type(),
                            (localizedPage.description() == null ? "" : localizedPage.description()) + " "
                                    + (localizedPage.extract() == null ? "" : localizedPage.extract()));
                    if (localized.status() == Status.MATCHED_HIGH_CONFIDENCE) return localized;
                    best = localized;
                }
            }
            for (String language : preferredLanguages(movie.title())) {
                WikiPage page = fetchSummary(language, movie.title());
                if (page == null) continue;
                PosterCandidate candidate = page.candidate(language, false);
                LookupResult current = resolveDirect(movie, candidate, page.type(),
                        (page.description() == null ? "" : page.description()) + " "
                                + (page.extract() == null ? "" : page.extract()));
                if (current.status() == Status.MATCHED_HIGH_CONFIDENCE) return current;
                if (best == null || priority(current.status()) > priority(best.status())) best = current;
            }
            if (best != null && best.status() == Status.NO_POSTER) return best;

            String language = containsNonAscii(movie.title()) ? "vi" : "en";
            List<PosterCandidate> fallback = search(language, movie.title());
            if (fallback.isEmpty()) return best == null ? LookupResult.notFound() : best;
            PosterCandidate candidate = chooseFallback(movie, fallback);
            if (!titleVariantMatches(movie.title(), candidate.title())
                    || !isValidPosterUrl(candidate.posterUrl()) || !isPortraitPoster(candidate)) {
                List<PosterCandidate> refined = search(language,
                        movie.title() + " " + movie.releaseYear() + " film");
                if (!refined.isEmpty()) candidate = chooseFallback(movie, refined);
            }
            if (!isValidPosterUrl(candidate.posterUrl())) {
                WikiPage exactPage = fetchSummary(language, candidate.title());
                if (exactPage != null) candidate = exactPage.candidate(language, true);
            }
            LookupResult exactFallback = resolveFallback(movie, candidate);
            if (exactFallback.status() == Status.MATCHED_HIGH_CONFIDENCE) return exactFallback;
            return LookupResult.review(candidate, "SEARCH_FALLBACK_NEEDS_REVIEW");
        }

        private WikiPage fetchSummary(String language, String title) throws Exception {
            URI uri = URI.create("https://" + language + ".wikipedia.org/api/rest_v1/page/summary/"
                    + pathEncode(title));
            HttpResponse<String> response = send(uri);
            if (response.statusCode() == 404) return null;
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new ProviderFailure("WIKIMEDIA_HTTP_" + response.statusCode());
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ProviderFailure("WIKIMEDIA_HTTP_" + response.statusCode());
            }
            try { return WikiPage.from(Json.object(Json.parse(response.body()))); }
            catch (RuntimeException e) { throw new ProviderFailure("WIKIMEDIA_INVALID_JSON"); }
        }

        private List<PosterCandidate> search(String language, String title) throws Exception {
            URI uri = URI.create("https://" + language + ".wikipedia.org/w/rest.php/v1/search/page?q="
                    + queryEncode(title) + "&limit=10");
            HttpResponse<String> response = send(uri);
            if (response.statusCode() == 404) return List.of();
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new ProviderFailure("WIKIMEDIA_HTTP_" + response.statusCode());
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ProviderFailure("WIKIMEDIA_HTTP_" + response.statusCode());
            }
            try {
                Map<String, Object> root = Json.object(Json.parse(response.body()));
                List<PosterCandidate> candidates = new ArrayList<>();
                for (Object item : Json.array(root.get("pages"))) {
                    Map<String, Object> page = Json.object(item);
                    Map<String, Object> thumbnail = page.get("thumbnail") instanceof Map<?, ?>
                            ? Json.object(page.get("thumbnail")) : Map.of();
                    candidates.add(new PosterCandidate(Json.string(page, "title"),
                            firstYear(Json.string(page, "description") + " " + Json.string(page, "excerpt")),
                            "https://" + language + ".wikipedia.org/wiki/" + pathEncode(Json.string(page, "title")),
                            absoluteUrl(Json.string(thumbnail, "url")), language,
                            Json.string(page, "description") + " " + Json.string(page, "excerpt"),
                            Json.integer(thumbnail.get("width"), 0), Json.integer(thumbnail.get("height"), 0), true));
                }
                return List.copyOf(candidates);
            } catch (RuntimeException e) { throw new ProviderFailure("WIKIMEDIA_INVALID_JSON"); }
        }

        private HttpResponse<String> send(URI uri) throws Exception {
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(WIKIMEDIA_REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .GET().build();
            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                throttleWikimedia();
                HttpResponse<String> response;
                try {
                    response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                } catch (IOException | InterruptedException e) {
                    if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                    if (attempt == MAX_ATTEMPTS) throw new ApiFailure("Wikimedia request failed after bounded retries");
                    sleepBackoff(attempt);
                    continue;
                }
                lastRequestMillis = System.currentTimeMillis();
                if (isTransient(response.statusCode()) && attempt < MAX_ATTEMPTS) {
                    sleepProviderBackoff(response, attempt);
                    continue;
                }
                return response;
            }
            throw new ApiFailure("Wikimedia request failed after bounded retries");
        }

        private void throttleWikimedia() throws InterruptedException {
            long wait = WIKIMEDIA_MIN_INTERVAL_MILLIS - (System.currentTimeMillis() - lastRequestMillis);
            if (lastRequestMillis > 0 && wait > 0) Thread.sleep(wait);
        }

        private static void sleepProviderBackoff(HttpResponse<String> response, int attempt)
                throws InterruptedException {
            long retryAfterMillis = response.headers().firstValue("Retry-After")
                    .map(WikimediaGateway::parseRetryAfterMillis).orElse(0L);
            long fallbackMillis = response.statusCode() == 429 ? 3_000L * attempt : 1_000L * attempt;
            Thread.sleep(Math.max(fallbackMillis, retryAfterMillis));
        }

        private static long parseRetryAfterMillis(String value) {
            try {
                long seconds = Long.parseLong(value.trim());
                return Math.max(0L, seconds * 1_000L);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }

        private static PosterCandidate chooseFallback(Movie movie, List<PosterCandidate> candidates) {
            PosterCandidate best = candidates.get(0);
            int bestScore = score(movie, best);
            for (PosterCandidate candidate : candidates.subList(1, candidates.size())) {
                int score = score(movie, candidate);
                if (score > bestScore) { best = candidate; bestScore = score; }
            }
            return best;
        }

        private static LookupResult resolveFallback(Movie movie, PosterCandidate candidate) {
            if (candidate == null || !titleVariantMatches(movie.title(), candidate.title())) {
                return LookupResult.review(candidate, "SEARCH_TITLE_REQUIRES_REVIEW");
            }
            boolean yearMatches = Objects.equals(candidate.year(), movie.releaseYear())
                    || containsYear(candidate.description(), movie.releaseYear());
            boolean exactTitleWithoutAnyYear = titleMatches(movie.title(), candidate.title())
                    && candidate.year() == 0;
            if ((!yearMatches && !exactTitleWithoutAnyYear) || !hasFilmEvidence(candidate.description())
                    || !isValidPosterUrl(candidate.posterUrl()) || !isPortraitPoster(candidate)) {
                return LookupResult.review(candidate, "SEARCH_FALLBACK_NEEDS_REVIEW");
            }
            return new LookupResult(Status.MATCHED_HIGH_CONFIDENCE, candidate, "SEARCH_EXACT_TITLE_YEAR_FILM");
        }

        private static String absoluteUrl(String value) {
            return value != null && value.startsWith("//") ? "https:" + value : value;
        }

        private static int score(Movie movie, PosterCandidate candidate) {
            String normalized = normalizeTitle(candidate.title());
            boolean titleVariant = titleVariantMatches(movie.title(), candidate.title());
            boolean yearEvidence = Objects.equals(candidate.year(), movie.releaseYear())
                    || containsYear(candidate.description(), movie.releaseYear());
            boolean filmEvidence = hasFilmEvidence(candidate.description());
            if (titleVariant && yearEvidence && filmEvidence) return 130;
            if (titleVariant && filmEvidence) return 100;
            if (titleMatches(movie.title(), candidate.title())) return 80;
            return normalized.equals(normalizeTitle(movie.title()) + " film") ? 70 : 10;
        }

        private static List<String> preferredLanguages(String title) {
            // Cinevora's catalog is English/Vietnamese. Avoid a second provider
            // lookup for every miss; search fallback handles the primary locale.
            return containsNonAscii(title) ? List.of("vi") : List.of("en");
        }

        private static int priority(Status status) {
            return switch (status) {
                case MATCHED_HIGH_CONFIDENCE -> 5;
                case NO_POSTER -> 4;
                case REVIEW_REQUIRED -> 3;
                case AMBIGUOUS -> 2;
                case NOT_FOUND -> 1;
                default -> 0;
            };
        }

        private static boolean containsNonAscii(String value) {
            return value != null && value.codePoints().anyMatch(codePoint -> codePoint > 127);
        }
    }

    private record WikiPage(String type, String title, String description, String extract,
                            String pageUrl, String posterUrl, int width, int height) {
        static WikiPage from(Map<String, Object> root) {
            Map<String, Object> thumbnail = root.get("thumbnail") instanceof Map<?, ?>
                    ? Json.object(root.get("thumbnail")) : Map.of();
            Map<String, Object> urls = root.get("content_urls") instanceof Map<?, ?>
                    ? Json.object(root.get("content_urls")) : Map.of();
            Map<String, Object> desktop = urls.get("desktop") instanceof Map<?, ?>
                    ? Json.object(urls.get("desktop")) : Map.of();
            return new WikiPage(Json.string(root, "type"), Json.string(root, "title"),
                    Json.string(root, "description"), Json.string(root, "extract"), Json.string(desktop, "page"),
                    Json.string(thumbnail, "source"), Json.integer(thumbnail.get("width"), 0),
                    Json.integer(thumbnail.get("height"), 0));
        }

        PosterCandidate candidate(String language, boolean fallback) {
            String context = (description == null ? "" : description) + " "
                    + (extract == null ? "" : extract);
            return new PosterCandidate(title, firstYear(context), pageUrl, posterUrl, language,
                    context, width, height, fallback);
        }
    }

    private static int firstYear(String text) {
        if (text == null) return 0;
        Matcher matcher = YEAR_PATTERN.matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group()) : 0;
    }

    private static HttpResponse<String> sendWithRetries(HttpClient http, HttpRequest request,
                                                        boolean retryTransient) throws Exception {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HttpResponse<String> response = http.send(request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (retryTransient && isTransient(response.statusCode()) && attempt < MAX_ATTEMPTS) {
                    sleepBackoff(attempt); continue;
                }
                return response;
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                if (attempt == MAX_ATTEMPTS) throw new ApiFailure("Network request failed after bounded retries");
                sleepBackoff(attempt);
            }
        }
        throw new ApiFailure("Network request failed after bounded retries");
    }

    private static boolean isTransient(int status) { return status == 408 || status == 429 || status >= 500; }

    private static void sleepBackoff(int attempt) {
        try { Thread.sleep(250L * attempt); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static String pathEncode(String value) { return queryEncode(value).replace("+", "%20"); }
    private static String queryEncode(String value) { return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8); }

    private static String readGitCommit() {
        try {
            Process process = new ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            return process.waitFor() == 0 && !output.isBlank() ? output : "unknown";
        } catch (Exception e) { return "unknown"; }
    }

    private static void printConsoleReport(List<ImportResult> results, Path reportPath) {
        System.out.println();
        System.out.println("ID | Cinevora Title | Year | Wikimedia Title | Language | Result | Poster");
        System.out.println("---|---|---:|---|---|---|---");
        for (ImportResult result : results) {
            PosterCandidate candidate = result.candidate();
            System.out.printf(Locale.ROOT, "%s | %s | %s | %s | %s | %s | %s%n", result.movie().id(),
                    result.movie().title(), result.movie().releaseYear(), candidate == null ? "-" : dash(candidate.title()),
                    candidate == null ? "-" : candidate.language(), result.status(),
                    candidate != null && isValidPosterUrl(candidate.posterUrl()) ? "yes" : "no");
        }
        Map<Status, Integer> counts = counts(results);
        System.out.println();
        System.out.println("Summary:");
        System.out.println("TOTAL=" + results.size());
        for (Status status : Status.values()) System.out.println(status + "=" + counts.getOrDefault(status, 0));
        System.out.println("Report: " + reportPath);
    }

    private static final class Report {
        private Report() { }

        static String render(List<ImportResult> results, Options options, Config config, String commit) {
            Map<Status, Integer> counts = counts(results);
            StringBuilder out = new StringBuilder();
            out.append("# Wikimedia Poster Import Report\n\n");
            out.append("- Execution date: ").append(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.now().atOffset(ZoneOffset.UTC))).append("\n");
            out.append("- Cinevora commit: `").append(cell(commit)).append("`\n");
            out.append("- Provider: Wikimedia Page Content Service / Wikipedia REST API\n");
            out.append("- Mode: `").append(options.apply() ? "APPLY" : "DRY-RUN").append("`\n");
            out.append("- Force overwrite: `").append(options.force()).append("`\n");
            out.append("- Cinevora API: `").append(cell(config.cinevoraApiUrl())).append("`\n\n");
            out.append("## Summary\n\n| Metric | Count |\n|---|---:|\n");
            out.append(row("TOTAL", results.size()));
            for (Status status : Status.values()) out.append(row(status.name(), counts.getOrDefault(status, 0)));
            out.append("\n## Match decisions\n\n");
            out.append("| Cinevora ID | Cinevora title/year | Wikimedia title | Page language | Poster | Decision | Applied | Verified | Page | Poster URL | Reason |\n");
            out.append("|---:|---|---|---|---|---|---|---|---|---|---|\n");
            for (ImportResult result : results) {
                PosterCandidate candidate = result.candidate();
                String page = candidate == null ? "-" : cell(candidate.pageUrl());
                String verified = result.applied() ? (Boolean.TRUE.equals(result.verified()) ? "yes" : "no") : "-";
                String reason = result.verificationError() == null ? result.reason() : result.reason() + "; " + result.verificationError();
                String posterUrl = candidate == null ? "-" : cell(candidate.posterUrl());
                out.append('|').append(result.movie().id()).append('|')
                        .append(cell(result.movie().title() + " / " + result.movie().releaseYear())).append('|')
                        .append(candidate == null ? "-" : cell(candidate.title())).append('|')
                        .append(candidate == null ? "-" : candidate.language()).append('|')
                        .append(candidate != null && isValidPosterUrl(candidate.posterUrl()) ? "yes" : "no").append('|')
                        .append(result.status()).append('|').append(result.applied() ? "yes" : "no").append('|')
                        .append(verified).append('|').append(page).append('|').append(posterUrl).append('|')
                        .append(cell(dash(reason))).append("|\n");
            }
            out.append("\n## Apply verification\n\n");
            out.append(options.apply()
                    ? "Updated movies were reloaded through the Cinevora admin API and compared with the requested poster URL.\n"
                    : "Dry-run completed; no Cinevora movie update request was issued.\n");
            out.append("\nThis job changes only movie thumbnailUrl through the Cinevora admin API. Trailer URLs are managed separately by YouTubeTrailerImporter.\n");
            return out.toString();
        }

        private static String row(String label, int count) { return "| " + label + " | " + count + " |\n"; }
    }

    private static Map<Status, Integer> counts(List<ImportResult> results) {
        Map<Status, Integer> counts = new TreeMap<>((left, right) -> Integer.compare(left.ordinal(), right.ordinal()));
        for (ImportResult result : results) counts.merge(result.status(), 1, Integer::sum);
        return counts;
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new ConfigurationException(name + " is required.");
        return value.trim();
    }

    private static String optionalEnv(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String trimTrailingSlash(String value) {
        String result = value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private static Long positiveLong(String value, String name) {
        try { long parsed = Long.parseLong(value); if (parsed > 0) return parsed; }
        catch (NumberFormatException ignored) { }
        throw new ConfigurationException("--" + name + " must be a positive integer");
    }

    private static Integer positiveInt(String value, String name) {
        try { int parsed = Integer.parseInt(value); if (parsed > 0) return parsed; }
        catch (NumberFormatException ignored) { }
        throw new ConfigurationException("--" + name + " must be a positive integer");
    }

    private static boolean hasPoster(String value) {
        return isValidPosterUrl(value);
    }

    private static String dash(String value) { return value == null || value.isBlank() ? "-" : value; }
    private static String cell(String value) { return dash(value).replace("|", "\\|").replace('\n', ' ').replace('\r', ' '); }

    private static String safeMessage(Exception e) {
        return e.getMessage() == null || e.getMessage().isBlank() ? e.getClass().getSimpleName() : e.getMessage();
    }

    private static String safeCategory(Exception e) { return e instanceof ApiFailure ? safeMessage(e) : e.getClass().getSimpleName(); }

    private static final class ConfigurationException extends RuntimeException {
        private ConfigurationException(String message) { super(message); }
    }

    private static class ApiFailure extends Exception {
        private ApiFailure(String message) { super(message); }
    }

    private static final class ProviderFailure extends RuntimeException {
        private ProviderFailure(String message) { super(message); }
    }

    static final class Json {
        private Json() { }
        static Object parse(String json) { return new Parser(json).parse(); }
        static Map<String, Object> object(Object value) {
            if (!(value instanceof Map<?, ?> map)) throw new IllegalArgumentException("JSON object expected");
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) result.put(String.valueOf(entry.getKey()), entry.getValue());
            return result;
        }
        static List<Object> array(Object value) {
            if (!(value instanceof List<?> list)) throw new IllegalArgumentException("JSON array expected");
            return new ArrayList<>(list);
        }
        static String string(Map<String, Object> map, String key) {
            Object value = map.get(key); return value == null ? null : String.valueOf(value);
        }
        static long longValue(Object value) { return value instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(value)); }
        static Integer integer(Object value, Integer fallback) {
            if (value == null) return fallback; return value instanceof Number n ? n.intValue() : Integer.valueOf(String.valueOf(value));
        }
        static BigDecimal decimal(Object value) { return value == null ? null : new BigDecimal(String.valueOf(value)); }
        static boolean bool(Object value) { return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value)); }
        static String stringify(Object value) {
            if (value == null) return "null";
            if (value instanceof String string) return quote(string);
            if (value instanceof Number || value instanceof Boolean) return String.valueOf(value);
            if (value instanceof Map<?, ?> map) {
                StringBuilder out = new StringBuilder("{"); boolean first = true;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!first) out.append(','); first = false;
                    out.append(quote(String.valueOf(entry.getKey()))).append(':').append(stringify(entry.getValue()));
                }
                return out.append('}').toString();
            }
            throw new IllegalArgumentException("Unsupported JSON value");
        }
        static String quote(String value) {
            StringBuilder out = new StringBuilder("\"");
            value.codePoints().forEach(codePoint -> {
                switch (codePoint) {
                    case '"' -> out.append("\\\""); case '\\' -> out.append("\\\\"); case '\b' -> out.append("\\b");
                    case '\f' -> out.append("\\f"); case '\n' -> out.append("\\n"); case '\r' -> out.append("\\r");
                    case '\t' -> out.append("\\t"); default -> out.appendCodePoint(codePoint);
                }
            });
            return out.append('"').toString();
        }
        private static final class Parser {
            private final String input; private int index;
            private Parser(String input) { this.input = input; }
            private Object parse() { skip(); Object value = value(); skip(); if (index != input.length()) throw error("Trailing JSON"); return value; }
            private Object value() {
                skip(); if (index >= input.length()) throw error("Unexpected end");
                return switch (input.charAt(index)) {
                    case '{' -> objectValue(); case '[' -> arrayValue(); case '"' -> stringValue();
                    case 't' -> literal("true", true); case 'f' -> literal("false", false); case 'n' -> literal("null", null);
                    default -> numberValue();
                };
            }
            private Map<String, Object> objectValue() {
                expect('{'); Map<String, Object> result = new LinkedHashMap<>(); skip();
                if (peek('}')) { index++; return result; }
                while (true) { skip(); String key = stringValue(); skip(); expect(':'); result.put(key, value()); skip();
                    if (peek('}')) { index++; return result; } expect(','); }
            }
            private List<Object> arrayValue() {
                expect('['); List<Object> result = new ArrayList<>(); skip();
                if (peek(']')) { index++; return result; }
                while (true) { result.add(value()); skip(); if (peek(']')) { index++; return result; } expect(','); }
            }
            private String stringValue() {
                expect('"'); StringBuilder result = new StringBuilder();
                while (index < input.length()) { char c = input.charAt(index++); if (c == '"') return result.toString();
                    if (c != '\\') { result.append(c); continue; } if (index >= input.length()) throw error("Invalid escape");
                    char escape = input.charAt(index++); switch (escape) {
                        case '"', '\\', '/' -> result.append(escape); case 'b' -> result.append('\b'); case 'f' -> result.append('\f');
                        case 'n' -> result.append('\n'); case 'r' -> result.append('\r'); case 't' -> result.append('\t');
                        case 'u' -> { result.append((char) Integer.parseInt(input.substring(index, index + 4), 16)); index += 4; }
                        default -> throw error("Invalid escape"); }
                } throw error("Unterminated string");
            }
            private Object numberValue() {
                int start = index; while (index < input.length() && "-+0123456789.eE".indexOf(input.charAt(index)) >= 0) index++;
                String value = input.substring(start, index); try { return value.matches(".*[.eE].*") ? new BigDecimal(value) : Long.valueOf(value); }
                catch (NumberFormatException e) { throw error("Invalid number"); }
            }
            private Object literal(String text, Object value) { if (!input.startsWith(text, index)) throw error("Invalid literal"); index += text.length(); return value; }
            private void expect(char value) { if (index >= input.length() || input.charAt(index) != value) throw error("Expected " + value); index++; }
            private boolean peek(char value) { return index < input.length() && input.charAt(index) == value; }
            private void skip() { while (index < input.length() && Character.isWhitespace(input.charAt(index))) index++; }
            private IllegalArgumentException error(String message) { return new IllegalArgumentException(message + " at " + index); }
        }
    }
}
