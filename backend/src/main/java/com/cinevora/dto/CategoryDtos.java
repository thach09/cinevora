package com.cinevora.dto;

import com.cinevora.entity.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CategoryDtos {
    private CategoryDtos() {}
    public record Request(@NotBlank @Size(max = 100) String name, @Size(max = 2000) String description) {}
    public record Response(Long id, String name, String description, boolean active) {
        public static Response from(Category c) { return new Response(c.getId(), c.getName(), c.getDescription(), c.isActive()); }
    }
}
