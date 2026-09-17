package com.cinevora.service;

import com.cinevora.entity.Category;
import com.cinevora.exception.BusinessException;
import com.cinevora.repository.CategoryRepository;
import com.cinevora.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock CategoryRepository categories;
    @Mock MovieRepository movies;
    @InjectMocks CategoryService service;

    @Test void cannotSoftDeleteCategoryWithActiveMovies() {
        Category category = new Category();
        when(categories.findById(1L)).thenReturn(java.util.Optional.of(category));
        when(movies.countByCategory_IdAndActiveTrue(1L)).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service.delete(1L));
        verify(categories, never()).save(any());
    }
}
