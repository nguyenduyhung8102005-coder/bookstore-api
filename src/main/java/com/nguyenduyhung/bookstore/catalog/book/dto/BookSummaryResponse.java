package com.nguyenduyhung.bookstore.catalog.book.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Bản rút gọn dùng trong danh sách: bỏ mô tả dài để response nhẹ, và chỉ báo
 * còn hàng hay không thay vì số lượng tồn kho chính xác.
 */
public record BookSummaryResponse(
        Long id,
        String title,
        BigDecimal price,
        boolean inStock,
        String categoryName,
        List<String> authorNames
) {
}
