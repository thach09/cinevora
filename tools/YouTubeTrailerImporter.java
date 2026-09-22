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

/**
 * Curated legal-trailer importer for Cinevora.
 *
 * <p>The catalog contains official or authorized trailer pages only. The
 * importer writes the public watch/embed URL, never downloads a movie, and
 * never talks directly to PostgreSQL. Dry-run is the default.</p>
 */
public final class YouTubeTrailerImporter {
    private static final String DEFAULT_CINEVORA_API_URL = "http://localhost:8080/api/v1";
    private static final Path DEFAULT_REPORT = Path.of("docs", "verification", "TRAILER_IMPORT_REPORT.md");
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);
    private static final int MAX_ATTEMPTS = 2;
    private static final Map<String, TrailerCandidate> SOURCES = curatedSources();

    private YouTubeTrailerImporter() {
    }

    public static void main(String[] args) {
        try {
            Options options = Options.parse(args);
            Config config = Config.fromEnvironment();
            HttpClient http = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpCinevoraGateway cinevora = new HttpCinevoraGateway(http, config.cinevoraApiUrl(),
                    config.adminUsername(), config.adminPassword());
            String token = cinevora.login();
            List<Movie> movies = cinevora.loadActiveMovies(token);
            if (options.movieId() != null) movies = movies.stream()
                    .filter(movie -> movie.id().equals(options.movieId())).toList();
            if (options.limit() != null && movies.size() > options.limit()) movies = movies.subList(0, options.limit());

            System.out.println("Legal trailer import mode: " + (options.apply() ? "APPLY" : "DRY-RUN"));
            System.out.println("Movies loaded from Cinevora: " + movies.size());
            List<ImportResult> results = runImport(movies, new CuratedSourceProvider(), cinevora, token, options);
            if (options.apply()) verifyPersistedTrailers(cinevora, token, results);

            String report = Report.render(results, options, config, readGitCommit());
            Path parent = config.reportPath().getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(config.reportPath(), report, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            printConsoleReport(results, config.reportPath());
        } catch (ConfigurationException e) {
            System.err.println(e.getMessage());
            System.exit(2);
        } catch (Exception e) {
            System.err.println("Legal trailer import failed: " + safeMessage(e));
            System.exit(1);
        }
    }

    public interface TrailerProvider {
        LookupResult lookup(Movie movie);
    }

    public interface CinevoraGateway {
        void updateVideo(Movie movie, String videoUrl, String token) throws Exception;

        List<Movie> loadActiveMovies(String token) throws Exception;
    }

    public record Movie(Long id, Long categoryId, String title, String director, String actors,
                        Integer releaseYear, BigDecimal rating, Integer durationMinutes, String videoUrl,
                        String thumbnailUrl, String description, boolean active) {
        public Map<String, Object> updatePayload(String videoUrl) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("title", title);
            payload.put("categoryId", categoryId);
            payload.put("director", director);
            payload.put("actors", actors);
            payload.put("releaseYear", releaseYear);
            payload.put("rating", rating);
            payload.put("durationMinutes", durationMinutes);
            payload.put("videoUrl", videoUrl);
            payload.put("thumbnailUrl", thumbnailUrl);
            payload.put("description", description);
            return payload;
        }
    }

    public record TrailerCandidate(String title, String videoUrl, String sourceName, String sourceUrl,
                                   boolean directVideo) {
    }

    public enum Status {
        ALREADY_HAS_TRAILER,
        MATCHED_CURATED_SOURCE,
        REVIEW_REQUIRED,
        NOT_FOUND,
        ERROR
    }

    public record LookupResult(Status status, TrailerCandidate candidate, String reason) {
        static LookupResult matched(TrailerCandidate candidate) {
            return new LookupResult(Status.MATCHED_CURATED_SOURCE, candidate, "CURATED_OFFICIAL_OR_AUTHORIZED_TRAILER");
        }

        static LookupResult review(TrailerCandidate candidate, String reason) {
            return new LookupResult(Status.REVIEW_REQUIRED, candidate, reason);
        }

        static LookupResult notFound() {
            return new LookupResult(Status.NOT_FOUND, null, "NO_CURATED_LEGAL_TRAILER");
        }
    }

    public static final class ImportResult {
        private final Movie movie;
        private final Status status;
        private final TrailerCandidate candidate;
        private final String reason;
        private final boolean applied;
        private volatile Boolean verified;
        private volatile String verificationError;

        private ImportResult(Movie movie, Status status, TrailerCandidate candidate, String reason, boolean applied) {
            this.movie = movie;
            this.status = status;
            this.candidate = candidate;
            this.reason = reason;
            this.applied = applied;
        }

        static ImportResult skipped(Movie movie) {
            return new ImportResult(movie, Status.ALREADY_HAS_TRAILER, null, "EXISTING_VIDEO_URL", false);
        }

        static ImportResult fromLookup(Movie movie, LookupResult lookup) {
            return new ImportResult(movie, lookup.status(), lookup.candidate(), lookup.reason(), false);
        }

        static ImportResult error(Movie movie, String reason) {
            return new ImportResult(movie, Status.ERROR, null, reason, false);
        }

        ImportResult withApplied(boolean value) {
            return new ImportResult(movie, status, candidate, reason, value);
        }

        public Movie movie() { return movie; }
        public Status status() { return status; }
        public TrailerCandidate candidate() { return candidate; }
        public String reason() { return reason; }
        public boolean applied() { return applied; }
        public Boolean verified() { return verified; }
        public String verificationError() { return verificationError; }
        private void setVerified(boolean value) { verified = value; }
        private void setVerificationError(String value) { verificationError = value; }
    }

    public record Options(boolean apply, boolean force, Long movieId, Integer limit) {
        static Options parse(String[] args) {
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

    public static List<ImportResult> runImport(List<Movie> movies, TrailerProvider provider,
                                                CinevoraGateway cinevora, String token, Options options) {
        List<ImportResult> results = new ArrayList<>();
        for (Movie movie : movies) {
            if (!options.force() && hasVideo(movie.videoUrl())) {
                results.add(ImportResult.skipped(movie));
                continue;
            }
            try {
                LookupResult lookup = provider.lookup(movie);
                ImportResult result = ImportResult.fromLookup(movie, lookup);
                if (options.apply() && result.status() == Status.MATCHED_CURATED_SOURCE) {
                    cinevora.updateVideo(movie, result.candidate().videoUrl(), token);
                    result = result.withApplied(true);
                }
                results.add(result);
            } catch (Exception e) {
                results.add(ImportResult.error(movie, safeCategory(e)));
            }
        }
        return List.copyOf(results);
    }

    public static boolean hasVideo(String value) {
        return value != null && !value.isBlank();
    }

    public static boolean isValidTrailerUrl(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            URI uri = URI.create(value.trim());
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) return false;
            return isValidYouTubeUrl(value) || value.toLowerCase(Locale.ROOT).contains(".mp4");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isValidYouTubeUrl(String value) {
        String id = youtubeId(value);
        return id != null && id.matches("[A-Za-z0-9_-]{11}");
    }

    static String youtubeId(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            URI uri = URI.create(value.trim());
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
            if ("youtu.be".equals(host)) {
                String path = uri.getPath() == null ? "" : uri.getPath();
                return path.replaceFirst("^/", "").split("/", 2)[0];
            }
            if ("youtube.com".equals(host) || "youtube-nocookie.com".equals(host)) {
                String query = uri.getQuery();
                if (query != null) {
                    for (String part : query.split("&")) {
                        String[] pair = part.split("=", 2);
                        if (pair.length == 2 && "v".equals(pair[0])) return pair[1];
                    }
                }
                String path = uri.getPath() == null ? "" : uri.getPath();
                java.util.regex.Matcher matcher = java.util.regex.Pattern
                        .compile("^/(?:embed|shorts)/([^/?]+)").matcher(path);
                return matcher.find() ? matcher.group(1) : null;
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid URLs are reported as review/error by the importer.
        }
        return null;
    }

    private static final class CuratedSourceProvider implements TrailerProvider {
        @Override
        public LookupResult lookup(Movie movie) {
            TrailerCandidate candidate = SOURCES.get(normalizeTitle(movie.title()));
            if (candidate == null) return LookupResult.notFound();
            if (!isValidTrailerUrl(candidate.videoUrl())) return LookupResult.review(candidate, "SOURCE_URL_INVALID");
            return LookupResult.matched(candidate);
        }
    }

    private static Map<String, TrailerCandidate> curatedSources() {
        Map<String, TrailerCandidate> sources = new LinkedHashMap<>();
        add(sources, "A Quiet Place", "https://www.youtube.com/watch?v=WR7cc5t7tv8", "Paramount Pictures");
        add(sources, "Annabelle", "https://www.youtube.com/watch?v=paFgQNPGlsg", "Warner Bros.");
        add(sources, "Avatar", "https://www.youtube.com/watch?v=5PSNL1qE6VY", "20th Century Studios");
        add(sources, "Avatar: The Way of Water", "https://www.youtube.com/watch?v=_ciBXV3MGGc", "20th Century Studios UK");
        add(sources, "Avengers: Infinity War", "https://www.youtube.com/watch?v=6ZfuNTqbHE8", "Marvel Entertainment");
        add(sources, "Bóng Đè", "https://rubikpictures.com/wp-content/uploads/2023/06/Phim-Bong-De-Trailer-KC-18.03.2022.mp4", "Rubik Pictures official site");
        add(sources, "B\u1ee5i \u0110\u1eddi Ch\u1ee3 L\u1edbn", "https://www.youtube.com/watch?v=b9pw1sdwyYs", "Galaxy Studio");
        add(sources, "Captain America: Civil War", "https://www.youtube.com/watch?v=uVdV-lxRPFo", "Marvel Entertainment");
        add(sources, "Chàng Vợ Của Em", "https://www.youtube.com/watch?v=Yviv8cGA21g", "Hãng Phim Chánh Phương");
        add(sources, "Cua Lại Vợ Bầu", "https://www.youtube.com/watch?v=lhTtrS98uf4", "CGV Cinemas Vietnam");
        add(sources, "Doraemon: Nobita và Bản Giao Hưởng Địa Cầu", "https://www.youtube.com/watch?v=Yug8gbDd5EQ", "CGV Cinemas Vietnam");
        add(sources, "Dune: Part One", "https://www.youtube.com/watch?v=gApi7K5nBzs", "Warner Bros.");
        add(sources, "Em Chưa 18", "https://www.youtube.com/watch?v=_affkHceSj4", "CGV Cinemas Vietnam");
        add(sources, "Fast & Furious 7", "https://www.youtube.com/watch?v=NoXdfvT5JkQ", "Universal Pictures Australia");
        add(sources, "Frozen", "https://www.youtube.com/watch?v=TbQm5doF_Uc", "Walt Disney Animation Studios");
        add(sources, "Get Out", "https://www.youtube.com/watch?v=AHEl7Pji0f8", "Universal Pictures Australia");
        add(sources, "Guardians of the Galaxy", "https://www.youtube.com/watch?v=d96cjJhvlMA", "Marvel Entertainment");
        add(sources, "Hai Phượng", "https://www.youtube.com/watch?v=THXPCF6UHh8", "CGV Cinemas Vietnam");
        add(sources, "Home Alone", "https://www.youtube.com/watch?v=jEDaVHmw7r4", "20th Century Studios");
        add(sources, "IT", "https://www.youtube.com/watch?v=xKJmEC5ieOk", "Warner Bros.");
        add(sources, "Inception", "https://www.youtube.com/watch?v=YoHD9XEInc0", "Warner Bros.");
        add(sources, "Inside Out 2", "https://www.youtube.com/watch?v=LEjhY15eCx0", "Pixar");
        add(sources, "Interstellar", "https://www.youtube.com/watch?v=zSWdZVtXT7E", "Warner Bros. UK");
        add(sources, "John Wick 4", "https://www.youtube.com/watch?v=qEVUtrk8_B4", "Lionsgate Movies");
        add(sources, "Jurassic World", "https://www.youtube.com/watch?v=aJJrkyHas78", "Universal Pictures");
        add(sources, "Kung Fu Panda 4", "https://www.youtube.com/watch?v=_inKs4eeHiI", "Universal Pictures / DreamWorks Animation");
        add(sources, "Kẻ Ăn Hồn", "https://www.youtube.com/watch?v=xWh0g4rKGjI", "CGV Cinemas Vietnam");
        add(sources, "La La Land", "https://www.youtube.com/watch?v=0pdqf4P9MB8", "Lionsgate Movies");
        add(sources, "Lật Mặt 6: Tấm Vé Định Mệnh", "https://www.youtube.com/watch?v=CXEDG-Dmn3E", "Lý Hải Minh Hà / Lý Hải Production");
        add(sources, "Mai", "https://www.youtube.com/watch?v=ckf4lDFyL8w", "3388 Films / Tran Thanh Town");
        add(sources, "Maika: Cô Bé Đến Từ Hành Tinh Khác", "https://www.youtube.com/watch?v=d3avzpHOHBk", "Beta Cinemas");
        add(sources, "Me Before You", "https://www.youtube.com/watch?v=Eh993__rOxA", "Warner Bros.");
        add(sources, "Minions", "https://www.youtube.com/watch?v=eisKxhjBnZ0", "Illumination");
        add(sources, "Minions: Sự Trỗi Dậy Của Gru", "https://www.youtube.com/watch?v=6DxjJzmYsXo", "Illumination");
        add(sources, "Mr. Bean's Holiday", "https://www.youtube.com/watch?v=LZfIzJ6XwPQ", "Universal Pictures At Home");
        add(sources, "Mắt Biếc", "https://www.youtube.com/watch?v=ITlQ0oU7tDA", "Galaxy Studio");
        add(sources, "Nhà Bà Nữ", "https://www.youtube.com/watch?v=IkaP0KJWTsQ", "TRẤN THÀNH TOWN");
        add(sources, "Quả Tim Máu", "https://www.youtube.com/watch?v=eFUOOcTZI_4", "Galaxy Studio");
        add(sources, "Spider-Man: No Way Home", "https://www.youtube.com/watch?v=JfVOs4VSpmA", "Sony Pictures Entertainment");
        add(sources, "Spirited Away", "https://www.youtube.com/watch?v=GAp2_0JJskk", "GKIDS Films");
        add(sources, "Thanh Sói: Cúc Dại Trong Đêm", "https://www.youtube.com/watch?v=v5Pka1nRMnI", "Ngo Thanh Van Official / Studio68");
        add(sources, "Thất Sơn Tâm Linh", "https://www.youtube.com/watch?v=GlrCgPsO-sI", "Galaxy Studio");
        add(sources, "The Hangover", "https://www.youtube.com/watch?v=tcdUhdOlz9M", "Warner Bros. / authorized classic trailer");
        add(sources, "The Conjuring", "https://www.youtube.com/watch?v=ejMMn0t58Lc", "Rotten Tomatoes Trailers / authorized trailer");
        add(sources, "The Lion King", "https://www.youtube.com/watch?v=7TavVZMewpY", "Disney");
        add(sources, "The Matrix", "https://www.youtube.com/watch?v=vKQi3bBA1y8", "Warner Bros.");
        add(sources, "The Notebook", "https://www.youtube.com/watch?v=BjJcYdEOI0k", "Rotten Tomatoes Classic Trailers");
        add(sources, "The Nun", "https://www.youtube.com/watch?v=pzD9zGcUNrw", "Warner Bros.");
        add(sources, "Titanic", "https://www.youtube.com/watch?v=oHY7D7K58BM", "Paramount Pictures");
        add(sources, "Toy Story 4", "https://www.youtube.com/watch?v=wmiIUN-7qhE", "Pixar");
        add(sources, "Your Name", "https://www.youtube.com/watch?v=xU47nhruN-Q", "Toho / authorized trailer");
        add(sources, "Zootopia", "https://www.youtube.com/watch?v=jWM0ct-OLsM", "Walt Disney Animation Studios");
        add(sources, "G\u00e1i Gi\u00e0 L\u1eafm Chi\u00eau 3", "https://www.youtube.com/watch?v=Vw-gr7Kg2UI", "CGV Cinemas Vietnam");
        add(sources, "Th\u00e1m T\u1eed L\u1eebng Danh Conan: Ng\u00f4i Sao 5 C\u00e1nh 1 Tri\u1ec7u \u0110\u00f4", "https://www.youtube.com/watch?v=C4pG3GbhQZw", "CGV Cinemas Vietnam");
        add(sources, "Th\u00e1ng N\u0103m R\u1ef1c R\u1ee1", "https://www.youtube.com/watch?v=_4eikzZlg4U", "CJ HK Entertainment");
        add(sources, "T\u00e8o Em", "https://www.youtube.com/watch?v=oRvzM1eX5ow", "CGV Cinemas Vietnam");
        add(sources, "Si\u00eau L\u1eeba G\u1eb7p Si\u00eau L\u1ea7y", "https://www.youtube.com/watch?v=aYu21Smdq9I", "3388 Films");
        add(sources, "Kung Fu Hustle", "https://www.youtube.com/watch?v=PRbPXbgsKyE", "Smithsonian Institution / authorized event trailer");
        add(sources, "Transformers: Dark of the Moon", "https://www.youtube.com/watch?v=EkqdO8dhptc", "Paramount Movies");
        return Map.copyOf(sources);
    }

    private static void add(Map<String, TrailerCandidate> sources, String title, String videoUrl, String sourceName) {
        sources.put(normalizeTitle(title), new TrailerCandidate(title, videoUrl, sourceName, videoUrl,
                !isValidYouTubeUrl(videoUrl)));
    }

    public static String normalizeTitle(String value) {
        if (value == null) return "";
        String lower = value.trim().toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();
        for (int offset = 0; offset < lower.length();) {
            int codePoint = lower.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.isLetterOrDigit(codePoint)) {
                while (!result.isEmpty() && result.charAt(result.length() - 1) == ' ') result.setLength(result.length() - 1);
                result.appendCodePoint(codePoint);
            } else if (!result.isEmpty()) result.append(' ');
        }
        return result.toString().replaceAll("\\s+", " ").trim();
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
                    WikimediaPosterImporter.Json.stringify(Map.of("username", username, "password", password)));
            requireSuccess(response, "Cinevora login");
            Map<String, Object> root = WikimediaPosterImporter.Json.object(WikimediaPosterImporter.Json.parse(response.body()));
            String token = WikimediaPosterImporter.Json.string(
                    WikimediaPosterImporter.Json.object(root.get("data")), "token");
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
                Map<String, Object> data = WikimediaPosterImporter.Json.object(
                        WikimediaPosterImporter.Json.object(WikimediaPosterImporter.Json.parse(response.body())).get("data"));
                for (Object item : WikimediaPosterImporter.Json.array(data.get("content"))) {
                    movies.add(parseMovie(WikimediaPosterImporter.Json.object(item)));
                }
                totalPages = WikimediaPosterImporter.Json.integer(data.get("totalPages"), page + 1);
                page++;
                if (page > 10000) throw new ApiFailure("Cinevora pagination exceeded safety limit");
            } while (page < totalPages);
            return List.copyOf(movies);
        }

        @Override
        public void updateVideo(Movie movie, String videoUrl, String token) throws Exception {
            if (!isValidTrailerUrl(videoUrl)) throw new ApiFailure("Invalid trailer URL for movie " + movie.id());
            HttpResponse<String> response = request("PUT", baseUrl + "/movies/" + movie.id(), token,
                    WikimediaPosterImporter.Json.stringify(movie.updatePayload(videoUrl)));
            requireSuccess(response, "Cinevora update movie " + movie.id());
        }

        private HttpResponse<String> request(String method, String url, String token, String body) throws Exception {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(HTTP_TIMEOUT)
                    .header("Accept", "application/json");
            if (token != null) builder.header("Authorization", "Bearer " + token);
            if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
            else builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            return sendWithRetries(http, builder.build());
        }

        private static Movie parseMovie(Map<String, Object> object) {
            return new Movie(WikimediaPosterImporter.Json.longValue(object.get("id")),
                    WikimediaPosterImporter.Json.longValue(object.get("categoryId")),
                    WikimediaPosterImporter.Json.string(object, "title"),
                    WikimediaPosterImporter.Json.string(object, "director"),
                    WikimediaPosterImporter.Json.string(object, "actors"),
                    WikimediaPosterImporter.Json.integer(object.get("releaseYear"), null),
                    WikimediaPosterImporter.Json.decimal(object.get("rating")),
                    WikimediaPosterImporter.Json.integer(object.get("durationMinutes"), null),
                    WikimediaPosterImporter.Json.string(object, "videoUrl"),
                    WikimediaPosterImporter.Json.string(object, "thumbnailUrl"),
                    WikimediaPosterImporter.Json.string(object, "description"),
                    WikimediaPosterImporter.Json.bool(object.get("active")));
        }

        private static void requireSuccess(HttpResponse<String> response, String operation) throws ApiFailure {
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiFailure(operation + " returned HTTP " + response.statusCode());
            }
            Map<String, Object> root;
            try { root = WikimediaPosterImporter.Json.object(WikimediaPosterImporter.Json.parse(response.body())); }
            catch (RuntimeException e) { throw new ApiFailure(operation + " returned invalid JSON"); }
            if (!WikimediaPosterImporter.Json.bool(root.get("success"))) throw new ApiFailure(operation + " returned success=false");
        }
    }

    private static void verifyPersistedTrailers(CinevoraGateway cinevora, String token, List<ImportResult> results) {
        Map<Long, String> expected = new HashMap<>();
        for (ImportResult result : results) if (result.applied() && result.candidate() != null) {
            expected.put(result.movie().id(), result.candidate().videoUrl());
        }
        if (expected.isEmpty()) return;
        try {
            Map<Long, Movie> reloaded = new HashMap<>();
            for (Movie movie : cinevora.loadActiveMovies(token)) reloaded.put(movie.id(), movie);
            for (ImportResult result : results) if (result.applied()) {
                Movie movie = reloaded.get(result.movie().id());
                result.setVerified(movie != null && Objects.equals(movie.videoUrl(), expected.get(result.movie().id())));
            }
        } catch (Exception e) {
            for (ImportResult result : results) if (result.applied()) result.setVerificationError(safeCategory(e));
        }
    }

    private record Config(String cinevoraApiUrl, String adminUsername, String adminPassword, Path reportPath) {
        static Config fromEnvironment() {
            return new Config(normalizeApiUrl(optionalEnv("CINEVORA_API_URL", DEFAULT_CINEVORA_API_URL)),
                    requiredEnv("CINEVORA_ADMIN_USERNAME"), requiredEnv("CINEVORA_ADMIN_PASSWORD"),
                    Path.of(optionalEnv("TRAILER_IMPORT_REPORT_PATH", DEFAULT_REPORT.toString())));
        }
    }

    static String normalizeApiUrl(String value) {
        String result = value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result.endsWith("/api/v1") ? result : result + "/api/v1";
    }

    private static HttpResponse<String> sendWithRetries(HttpClient http, HttpRequest request) throws Exception {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if ((response.statusCode() == 408 || response.statusCode() == 429 || response.statusCode() >= 500)
                        && attempt < MAX_ATTEMPTS) {
                    Thread.sleep(250L * attempt);
                    continue;
                }
                return response;
            } catch (Exception e) {
                if (attempt == MAX_ATTEMPTS) throw e;
                Thread.sleep(250L * attempt);
            }
        }
        throw new ApiFailure("Network request failed after bounded retries");
    }

    private static final class Report {
        static String render(List<ImportResult> results, Options options, Config config, String commit) {
            Map<Status, Integer> counts = counts(results);
            StringBuilder out = new StringBuilder("# Legal Trailer Import Report\n\n");
            out.append("- Execution date: ").append(DateTimeFormatter.ISO_OFFSET_DATE_TIME
                    .format(Instant.now().atOffset(ZoneOffset.UTC))).append('\n');
            out.append("- Cinevora commit: `").append(cell(commit)).append("`\n");
            out.append("- Source policy: official/authorized trailer pages; no full-movie URLs\n");
            out.append("- Mode: `").append(options.apply() ? "APPLY" : "DRY-RUN").append("`\n");
            out.append("- Force overwrite: `").append(options.force()).append("`\n");
            out.append("- Cinevora API: `").append(cell(config.cinevoraApiUrl())).append("`\n\n");
            out.append("## Summary\n\n| Metric | Count |\n|---|---:|\n| TOTAL | ").append(results.size()).append(" |\n");
            for (Status status : Status.values()) out.append('|').append(status).append('|')
                    .append(counts.getOrDefault(status, 0)).append("|\n");
            out.append("\n## Decisions\n\n| ID | Cinevora title | Source | Video URL | Decision | Applied | Verified | Reason |\n");
            out.append("|---:|---|---|---|---|---|---|---|\n");
            for (ImportResult result : results) {
                TrailerCandidate candidate = result.candidate();
                String verified = result.applied() ? (Boolean.TRUE.equals(result.verified()) ? "yes" : "no") : "-";
                String reason = result.verificationError() == null ? result.reason()
                        : result.reason() + "; " + result.verificationError();
                out.append('|').append(result.movie().id()).append('|')
                        .append(cell(result.movie().title())).append('|')
                        .append(candidate == null ? "-" : cell(candidate.sourceName())).append('|')
                        .append(candidate == null ? "-" : cell(candidate.videoUrl())).append('|')
                        .append(result.status()).append('|').append(result.applied() ? "yes" : "no").append('|')
                        .append(verified).append('|').append(cell(reason)).append("|\n");
            }
            out.append("\nYouTube rows are stored as public watch URLs and rendered through the YouTube privacy-enhanced embed. ");
            out.append("The direct MP4 row is hosted by the movie production site. Existing `videoUrl` values are skipped by default.\n");
            return out.toString();
        }
    }

    private static Map<Status, Integer> counts(List<ImportResult> results) {
        Map<Status, Integer> counts = new TreeMap<>((left, right) -> Integer.compare(left.ordinal(), right.ordinal()));
        for (ImportResult result : results) counts.merge(result.status(), 1, Integer::sum);
        return counts;
    }

    private static void printConsoleReport(List<ImportResult> results, Path reportPath) {
        System.out.println();
        System.out.println("ID | Cinevora Title | Source | Result | Applied | Verified");
        System.out.println("---|---|---|---|---|---");
        for (ImportResult result : results) {
            TrailerCandidate candidate = result.candidate();
            System.out.printf(Locale.ROOT, "%s | %s | %s | %s | %s | %s%n", result.movie().id(),
                    result.movie().title(), candidate == null ? "-" : candidate.sourceName(), result.status(),
                    result.applied(), result.applied() ? result.verified() : "-");
        }
        Map<Status, Integer> counts = counts(results);
        System.out.println();
        System.out.println("Summary:");
        System.out.println("TOTAL=" + results.size());
        for (Status status : Status.values()) System.out.println(status + "=" + counts.getOrDefault(status, 0));
        System.out.println("Report: " + reportPath);
    }

    private static String readGitCommit() {
        try {
            Process process = new ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            return process.waitFor() == 0 && !output.isBlank() ? output : "unknown";
        } catch (Exception e) { return "unknown"; }
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

    private static String cell(String value) {
        if (value == null || value.isBlank()) return "-";
        return value.replace("|", "\\|").replace('\n', ' ').replace('\r', ' ');
    }

    private static String safeMessage(Exception e) {
        return e.getMessage() == null || e.getMessage().isBlank() ? e.getClass().getSimpleName() : e.getMessage();
    }

    private static String safeCategory(Exception e) { return e instanceof ApiFailure ? safeMessage(e) : e.getClass().getSimpleName(); }

    private static final class ConfigurationException extends RuntimeException {
        private ConfigurationException(String message) { super(message); }
    }

    private static final class ApiFailure extends Exception {
        private ApiFailure(String message) { super(message); }
    }
}
