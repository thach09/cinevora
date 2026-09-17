package com.cinevora.service;

import com.cinevora.dto.CategoryDtos;
import com.cinevora.entity.Category;
import com.cinevora.exception.*;
import com.cinevora.repository.CategoryRepository;
import com.cinevora.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categories;
    private final MovieRepository movies;
    public CategoryService(CategoryRepository categories, MovieRepository movies) { this.categories = categories; this.movies = movies; }
    @Transactional(readOnly = true) public List<CategoryDtos.Response> list(boolean includeInactive) {
        return categories.findAll().stream().filter(c -> includeInactive || c.isActive()).sorted(java.util.Comparator.comparing(Category::getName)).map(CategoryDtos.Response::from).toList();
    }
    @Transactional public CategoryDtos.Response create(CategoryDtos.Request request) {
        if (categories.existsByNameIgnoreCase(request.name().trim())) throw new BusinessException("Tên thể loại đã tồn tại");
        Category c = new Category(); c.setName(request.name().trim()); c.setDescription(clean(request.description())); return CategoryDtos.Response.from(categories.save(c));
    }
    @Transactional public CategoryDtos.Response update(Long id, CategoryDtos.Request request) {
        Category c = get(id); if (!c.getName().equalsIgnoreCase(request.name().trim()) && categories.existsByNameIgnoreCase(request.name().trim())) throw new BusinessException("Tên thể loại đã tồn tại");
        c.setName(request.name().trim()); c.setDescription(clean(request.description())); return CategoryDtos.Response.from(c);
    }
    @Transactional public void delete(Long id) { Category c = get(id); if (movies.countByCategory_IdAndActiveTrue(id) > 0) throw new BusinessException("Không thể xóa thể loại khi vẫn còn phim đang liên kết"); c.setActive(false); }
    @Transactional public CategoryDtos.Response restore(Long id) { Category c = get(id); c.setActive(true); return CategoryDtos.Response.from(c); }
    public Category get(Long id) { return categories.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thể loại " + id)); }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
