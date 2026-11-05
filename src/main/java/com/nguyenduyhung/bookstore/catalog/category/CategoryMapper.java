package com.nguyenduyhung.bookstore.catalog.category;

import com.nguyenduyhung.bookstore.catalog.category.dto.CategoryResponse;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug());
    }
}
