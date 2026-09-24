import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { categoryApi, discoveryApi, getApiError, movieApi } from "../lib/api";
import { MovieGrid } from "../components/MovieCard";
import { Button, EmptyState, QueryError, Spinner } from "../components/ui";
import { formatNumber } from "../lib/format";
import { Seo } from "../components/Seo";
import { useAuthStore } from "../store/authStore";
import { resolveMediaUrl } from "../lib/environment";
import { useI18n } from "../lib/i18n";

export function BrowseMoviePage() {
  const { t, locale, categoryName } = useI18n();
  const [page, setPage] = useState(0);
  const navigate = useNavigate();
  const activeProfileId = useAuthStore((state) => state.activeProfileId);
  const categories = useQuery({
    queryKey: ["categories"],
    queryFn: categoryApi.list,
  });
  const movies = useQuery({
    queryKey: ["movies", "browse", page],
    queryFn: () =>
      movieApi.list({ page, size: 12, sort: "popularity", direction: "desc" }),
  });
  const trending = useQuery({
    queryKey: ["movies", "trending"],
    queryFn: () => movieApi.trending({ size: 5 }),
  });
  const home = useQuery({
    queryKey: ["home", activeProfileId],
    queryFn: discoveryApi.home,
    enabled: Boolean(activeProfileId),
    staleTime: 60_000,
  });

  if (movies.isLoading) return <Spinner label={t("browse.loading")} />;
  if (movies.isError) return <QueryError message={getApiError(movies.error)} />;

  const featured = trending.data?.content[0] || movies.data?.content[0];
  return (
    <div className="space-y-12">
      <Seo
        title={`${t("nav.browse")} · Cinevora`}
        description={t("browse.fallbackDescription")}
      />
      <section className="hero-banner">
        <HeroArtwork movie={featured} />
        <div className="hero-copy">
          <p className="eyebrow text-pink-200">{t("browse.recommendation")}</p>
          <h2>{featured?.title || t("browse.fallbackTitle")}</h2>
          <p className="hero-description">
            {featured?.description ||
              t("browse.fallbackDescription")}
          </p>
          <div className="mt-7 flex flex-wrap gap-3">
            <Button
              onClick={() => featured && navigate(`/movies/${featured.id}`)}
            >
              {t("browse.details")}
            </Button>
            <Button
              variant="secondary"
              onClick={() =>
                document
                  .getElementById("catalogue")
                  ?.scrollIntoView({ behavior: "smooth" })
              }
            >
              {t("browse.explore")}
            </Button>
          </div>
        </div>
        {featured && (
          <div className="hero-meta">
            <span className="hero-score">★ {featured.rating.toFixed(1)}</span>
            <span>{featured.releaseYear}</span>
            <span>{categoryName(featured.categoryName)}</span>
            {featured.durationMinutes && (
              <span>{t("common.minutes", { count: featured.durationMinutes })}</span>
            )}
            <span>{t("common.views", { count: formatNumber(featured.views, locale) })}</span>
          </div>
        )}
      </section>

      <section id="catalogue" className="space-y-6">
        <div className="section-heading">
          <div>
            <p className="eyebrow">{t("browse.popular")}</p>
            <h2 className="section-title">{t("browse.catalogueTitle")}</h2>
          </div>
          <Link
            className="text-sm font-semibold text-pink-300 transition hover:text-pink-200"
            to="/search"
          >
            {t("browse.advanced")} <span aria-hidden="true">→</span>
          </Link>
        </div>
        <div className="category-pills" aria-label={t("browse.categories")}>
          <Link to="/browse" className="category-pill category-pill-active">
            {t("browse.allTitles")}
          </Link>
          {categories.data?.map((category) => (
            <Link
              key={category.id}
              to={`/search?categoryId=${category.id}`}
              className="category-pill"
            >
              {categoryName(category.name)}
            </Link>
          ))}
        </div>
        {movies.data?.content.length ? (
          <MovieGrid movies={movies.data.content} />
        ) : (
          <EmptyState
            title={t("browse.emptyTitle")}
            description={t("browse.emptyDescription")}
          />
        )}
      </section>
      {home.data?.topPicks.length ? (
        <section className="space-y-6">
          <div className="section-heading">
            <div>
              <p className="eyebrow">{t("browse.forProfile")}</p>
              <h2 className="section-title">{t("browse.topPicks")}</h2>
            </div>
            <span className="section-note">{t("browse.tasteNote")}</span>
          </div>
          <MovieGrid movies={home.data.topPicks} />
        </section>
      ) : null}
      {home.data?.continueWatching.length ? (
        <section className="space-y-6">
          <div className="section-heading">
            <div>
              <p className="eyebrow">{t("library.continueEyebrow")}</p>
              <h2 className="section-title">{t("browse.resume")}</h2>
            </div>
            <Link
              className="text-sm font-semibold text-pink-300 transition hover:text-pink-200"
              to="/continue-watching"
            >
              {t("common.viewAll")} <span aria-hidden="true">→</span>
            </Link>
          </div>
          <MovieGrid
            movies={home.data.continueWatching.map((entry) => ({
              movieId: entry.movieId,
              title: entry.title,
              thumbnailUrl: entry.thumbnailUrl,
              addedAt: entry.updatedAt,
            }))}
          />
        </section>
      ) : null}
      <Pagination
        page={page}
        totalPages={movies.data?.totalPages || 0}
        onChange={setPage}
      />
    </div>
  );
}

function HeroArtwork({
  movie,
}: {
  movie?: { title: string; thumbnailUrl: string | null };
}) {
  const [failed, setFailed] = useState(!movie?.thumbnailUrl);
  useEffect(() => setFailed(!movie?.thumbnailUrl), [movie?.thumbnailUrl]);
  return (
    <div className="hero-media" aria-hidden="true">
      <div className="hero-art-fallback">
        <span>{movie?.title.slice(0, 1).toUpperCase() || "C"}</span>
      </div>
      {movie?.thumbnailUrl && !failed && (
        <img src={resolveMediaUrl(movie.thumbnailUrl)} alt="" onError={() => setFailed(true)} />
      )}
    </div>
  );
}

export function SearchPage() {
  const { t, categoryName } = useI18n();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const activeProfileId = useAuthStore((state) => state.activeProfileId);
  const [q, setQ] = useState(searchParams.get("q") || "");
  const [categoryId, setCategoryId] = useState(
    searchParams.get("categoryId") || "",
  );
  const [minRating, setMinRating] = useState("");
  const [sort, setSort] = useState("popularity");
  const [page, setPage] = useState(0);
  const [suggestionQuery, setSuggestionQuery] = useState(q.trim());
  const categories = useQuery({
    queryKey: ["categories"],
    queryFn: categoryApi.list,
  });
  const recentSearches = useQuery({
    queryKey: ["search-history", activeProfileId],
    queryFn: discoveryApi.recentSearches,
    enabled: Boolean(activeProfileId),
  });
  const popularSearches = useQuery({
    queryKey: ["popular-searches"],
    queryFn: () => discoveryApi.popularSearches(6),
  });
  useEffect(() => {
    const timer = window.setTimeout(() => setSuggestionQuery(q.trim()), 300);
    return () => window.clearTimeout(timer);
  }, [q]);
  const suggestions = useQuery({
    queryKey: ["movie-suggestions", suggestionQuery],
    queryFn: () => movieApi.suggestions(suggestionQuery),
    enabled: suggestionQuery.length >= 2,
    staleTime: 30_000,
  });
  const query = {
    q: q || undefined,
    categoryId: categoryId ? Number(categoryId) : undefined,
    minRating: minRating ? Number(minRating) : undefined,
    page,
    size: 12,
    sort,
    direction: "desc" as const,
  };
  const movies = useQuery({
    queryKey: ["movies", "search", query],
    queryFn: () => movieApi.list(query),
  });
  const rememberSearch = () => {
    const value = q.trim();
    if (value.length >= 2) {
      void discoveryApi.saveSearch(value).then(() => recentSearches.refetch());
    }
  };
  const submit = (event: FormEvent) => {
    event.preventDefault();
    rememberSearch();
    setPage(0);
    const params = new URLSearchParams();
    if (q) params.set("q", q);
    if (categoryId) params.set("categoryId", categoryId);
    navigate(`/search${params.toString() ? `?${params.toString()}` : ""}`, {
      replace: true,
    });
  };

  return (
    <div className="space-y-8">
      <Seo
        title={
          q ? `${t("search.title")}: ${q} · Cinevora` : `${t("search.seo")} · Cinevora`
        }
        description={t("search.copy")}
      />
      <div>
        <p className="eyebrow">{t("search.eyebrow")}</p>
        <h2 className="section-title mt-1">{t("search.title")}</h2>
        <p className="mt-2 max-w-2xl text-slate-400">
          {t("search.copy")}
        </p>
      </div>
      <form className="search-panel" onSubmit={submit}>
        {suggestions.data?.length && suggestionQuery.length >= 2 ? (
          <div
            className="suggestion-menu suggestion-menu-inline"
            role="listbox"
          >
            {suggestions.data.map((suggestion) => (
              <button
                type="button"
                key={suggestion.id}
                className="suggestion-item"
                onClick={() => {
                  void discoveryApi.saveSearch(suggestion.title);
                  navigate(`/movies/${suggestion.id}`);
                }}
              >
                <span>{suggestion.title}</span>
                <small>{suggestion.releaseYear}</small>
              </button>
            ))}
          </div>
        ) : null}
        <div className="search-input-wrap">
          <span aria-hidden="true">⌕</span>
          <input
            value={q}
            onChange={(event) => setQ(event.target.value)}
            placeholder={t("search.placeholder")}
            aria-label={t("search.inputLabel")}
          />
        </div>
        <select
          className="input filter-input"
          value={categoryId}
          onChange={(event) => {
            setCategoryId(event.target.value);
            setPage(0);
          }}
          aria-label={t("search.categoryLabel")}
        >
          <option value="">{t("search.allCategories")}</option>
          {categories.data?.map((category) => (
            <option key={category.id} value={category.id}>
              {categoryName(category.name)}
            </option>
          ))}
        </select>
        <select
          className="input filter-input"
          value={minRating}
          onChange={(event) => {
            setMinRating(event.target.value);
            setPage(0);
          }}
          aria-label={t("search.ratingLabel")}
        >
          <option value="">{t("search.anyRating")}</option>
          <option value="7">7+ {t("movie.rating")}</option>
          <option value="8">8+ {t("movie.rating")}</option>
          <option value="9">9+ {t("movie.rating")}</option>
        </select>
        <select
          className="input filter-input"
          value={sort}
          onChange={(event) => {
            setSort(event.target.value);
            setPage(0);
          }}
          aria-label={t("search.sortLabel")}
        >
          <option value="popularity">{t("search.popular")}</option>
          <option value="rating">{t("search.topRated")}</option>
          <option value="releaseYear">{t("search.newest")}</option>
          <option value="title">{t("search.titleAz")}</option>
        </select>
        <Button type="submit">{t("search.submit")}</Button>
      </form>
      {!q.trim() && (
        <div className="search-discovery-row">
          <div>
            <span className="eyebrow">{t("search.recent")}</span>
            <div className="search-chips">
              {recentSearches.data?.length ? (
                recentSearches.data.slice(0, 6).map((entry) => (
                  <button
                    key={entry.id}
                    className="search-chip"
                    type="button"
                    onClick={() => {
                      setQ(entry.query);
                      setPage(0);
                    }}
                  >
                    {entry.query}
                  </button>
                ))
              ) : (
                <span className="text-sm text-slate-500">
                  {t("search.recentEmpty")}
                </span>
              )}
            </div>
          </div>
          <div>
            <span className="eyebrow">{t("search.popularNow")}</span>
            <div className="search-chips">
              {popularSearches.data?.map((entry) => (
                <button
                  key={entry.query}
                  className="search-chip"
                  type="button"
                  onClick={() => {
                    setQ(entry.query);
                    setPage(0);
                  }}
                >
                  {entry.query} <small>{entry.count}</small>
                </button>
              ))}
            </div>
          </div>
        </div>
      )}
      {movies.isLoading ? (
        <Spinner label={t("search.loading")} />
      ) : movies.isError ? (
        <QueryError message={getApiError(movies.error)} />
      ) : movies.data?.content.length ? (
        <>
          <MovieGrid movies={movies.data.content} />
          <Pagination
            page={page}
            totalPages={movies.data.totalPages}
            onChange={setPage}
          />
        </>
      ) : (
        <EmptyState
          title={t("search.emptyTitle")}
          description={t("search.emptyDescription")}
        />
      )}
    </div>
  );
}

function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}) {
  const { t } = useI18n();
  if (totalPages < 2) return null;
  return (
    <div className="pagination">
      <Button
        variant="ghost"
        disabled={page === 0}
        onClick={() => onChange(page - 1)}
      >
        {t("common.previous")}
      </Button>
      <span className="text-slate-400">
        {t("common.pageOf", { page: page + 1, total: totalPages })}
      </span>
      <Button
        variant="ghost"
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        {t("common.next")}
      </Button>
    </div>
  );
}
