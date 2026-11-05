package com.nguyenduyhung.bookstore.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Danh mục mã lỗi của toàn hệ thống.
 *
 * Mỗi mã gồm: tên (chính là `code` trả về cho client), HTTP status và câu thông
 * báo mặc định. HTTP status cho biết LOẠI lỗi (400, 404, 409...), còn `code`
 * cho biết CHÍNH XÁC lỗi gì, ví dụ cùng là 409 nhưng EMAIL_ALREADY_EXISTS khác
 * BOOK_OUT_OF_STOCK, để frontend xử lý từng trường hợp.
 *
 * Mỗi giai đoạn sau sẽ bổ sung mã lỗi nghiệp vụ của mình vào đây.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ===== 400 - Request không hợp lệ =====
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Request validation failed"),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body"),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "Invalid request parameter"),

    // ===== 401 / 403 - Xác thực & phân quyền (dùng từ Giai đoạn 1) =====
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication is required"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),

    // ===== 404 / 405 / 409 / 415 =====
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "Endpoint not found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported for this endpoint"),
    DATA_CONFLICT(HttpStatus.CONFLICT, "Request conflicts with existing data"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content type not supported"),

    // ===== 500 =====
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
