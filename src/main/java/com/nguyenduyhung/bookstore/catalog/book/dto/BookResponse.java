package com.nguyenduyhung.bookstore.catalog.book.dto;

import com.nguyenduyhung.bookstore.catalog.author.dto.AuthorSummaryResponse;
import com.nguyenduyhung.bookstore.catalog.category.dto.CategoryResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Chi tiết đầy đủ của một cuốn sách. */
public record BookResponse(
        Long id,
        String title,
        String isbn,
        String description,
        BigDecimal price,
        int stockQuantity,
        Integer publishedYear,
        CategoryResponse category,
        List<AuthorSummaryResponse> authors,
        Instant createdAt,
        Instant updatedAt
) {
}
