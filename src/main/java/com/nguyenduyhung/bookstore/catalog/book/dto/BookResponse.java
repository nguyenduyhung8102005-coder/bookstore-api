package com.nguyenduyhung.bookstore.catalog.book.dto;

import com.nguyenduyhung.bookstore.catalog.author.dto.AuthorSummaryResponse;
import com.nguyenduyhung.bookstore.catalog.category.dto.CategoryResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Chi tiết đầy đủ của một cuốn sách.
 *
 * @param averageRating điểm trung bình (1 chữ số thập phân), null khi chưa có đánh giá
 * @param reviewCount   số lượt đánh giá
 */
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
        BigDecimal averageRating,
        long reviewCount,
        Instant createdAt,
        Instant updatedAt
) {
}
