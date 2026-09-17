package com.cinevora.repository;

import com.cinevora.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByActiveTrueOrderByNameAsc();
    Optional<Category> findByIdAndActiveTrue(Long id);
    boolean existsByNameIgnoreCase(String name);
    long countByActiveTrue();
}
