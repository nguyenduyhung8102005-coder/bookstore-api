package com.nguyenduyhung.bookstore.catalog.author.dto;

/** Bản rút gọn của tác giả, dùng khi nhúng vào thông tin sách (không cần bio dài). */
public record AuthorSummaryResponse(Long id, String name) {
}
