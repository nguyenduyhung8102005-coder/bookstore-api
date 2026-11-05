package com.nguyenduyhung.bookstore.common.response;

/**
 * Một lỗi gắn với một field cụ thể, ví dụ: { "field": "email", "message": "must not be blank" }.
 * Giúp frontend hiển thị lỗi ngay dưới ô nhập tương ứng.
 */
public record FieldErrorResponse(String field, String message) {
}
