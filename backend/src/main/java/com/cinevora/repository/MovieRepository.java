package com.cinevora.repository;

import com.cinevora.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findByIdAndActiveTrue(Long id);
    @Query(value = "select m from Movie m join fetch m.category c where m.active = true " +
            "and (:q = '' or lower(m.title) like lower(concat('%', :q, '%')) " +
            "or lower(m.director) like lower(concat('%', :q, '%')) " +
            "or lower(m.actors) like lower(concat('%', :q, '%'))) " +
            "and (:categoryId is null or c.id = :categoryId) " +
            "and (:minYear is null or m.releaseYear >= :minYear) " +
            "and (:maxYear is null or m.releaseYear <= :maxYear) " +
            "and (:minRating is null or m.rating >= :minRating)",
            countQuery = "select count(m) from Movie m where m.active = true " +
                    "and (:q = '' or lower(m.title) like lower(concat('%', :q, '%')) " +
                    "or lower(m.director) like lower(concat('%', :q, '%')) " +
                    "or lower(m.actors) like lower(concat('%', :q, '%'))) " +
                    "and (:categoryId is null or m.category.id = :categoryId) " +
                    "and (:minYear is null or m.releaseYear >= :minYear) " +
                    "and (:maxYear is null or m.releaseYear <= :maxYear) " +
                    "and (:minRating is null or m.rating >= :minRating)")
    Page<Movie> searchActive(@Param("q") String q, @Param("categoryId") Long categoryId,
                             @Param("minYear") Integer minYear, @Param("maxYear") Integer maxYear,
                             @Param("minRating") java.math.BigDecimal minRating, Pageable pageable);
    @Query(value = "select m from Movie m join fetch m.category c where (:includeInactive = true or m.active = true) and (:q = '' or lower(m.title) like lower(concat('%', :q, '%')) or lower(m.director) like lower(concat('%', :q, '%')) or lower(m.actors) like lower(concat('%', :q, '%'))) and (:categoryId is null or c.id = :categoryId)", countQuery = "select count(m) from Movie m where (:includeInactive = true or m.active = true) and (:q = '' or lower(m.title) like lower(concat('%', :q, '%')) or lower(m.director) like lower(concat('%', :q, '%')) or lower(m.actors) like lower(concat('%', :q, '%'))) and (:categoryId is null or m.category.id = :categoryId)")
    Page<Movie> searchAdmin(@Param("q") String q, @Param("categoryId") Long categoryId,
                            @Param("includeInactive") boolean includeInactive, Pageable pageable);
    long countByActiveTrue();
    long countByCategory_IdAndActiveTrue(Long categoryId);
    @Query("select m from Movie m join fetch m.category where m.active = true order by m.views desc")
    Page<Movie> findTrending(Pageable pageable);
    @Query("select distinct m from Movie m join fetch m.category where m.active = true")
    List<Movie> findActiveCandidates(Pageable pageable);
    @Query("select distinct m from Movie m join fetch m.category where m.active = true and m.id not in :excluded")
    List<Movie> findActiveCandidatesExcluding(@Param("excluded") java.util.Collection<Long> excluded, Pageable pageable);
}
