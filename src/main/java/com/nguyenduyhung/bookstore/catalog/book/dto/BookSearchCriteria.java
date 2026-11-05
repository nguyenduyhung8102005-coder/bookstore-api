package com.nguyenduyhung.bookstore.catalog.book.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Các tham số lọc của GET /api/v1/books, ví dụ:
 * ?keyword=dế mèn&categoryId=1&authorId=2&minPrice=50000&maxPrice=200000
 *
 * Spring MVC tự tạo record này từ query string (gọi constructor, tên tham số
 * trùng tên field). Mọi field đều không bắt buộc: null nghĩa là "không lọc theo tiêu chí này".
 */
public record BookSearchCriteria(
        @Size(max = 100)
        String keyword,

        @Positive
        Long categoryId,

        @Positive
        Long authorId,

        @DecimalMin("0")
        BigDecimal minPrice,

        @DecimalMin("0")
        BigDecimal maxPrice
) {
}
