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
    QUANTITY_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "Quantity exceeds the allowed limit"),

    // ===== 401 - Chưa xác thực / xác thực thất bại =====
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication is required"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email or password is incorrect"),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Access token has expired"),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "Access token is invalid"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh token is invalid, expired or revoked"),

    // ===== 403 - Đã xác thực nhưng không có quyền =====
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "Account is disabled"),

    // ===== 409 - Xung đột dữ liệu nghiệp vụ =====
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email is already registered"),
    CATEGORY_ALREADY_EXISTS(HttpStatus.CONFLICT, "A category with the same name already exists"),
    CATEGORY_IN_USE(HttpStatus.CONFLICT, "Category still has books and cannot be deleted"),
    AUTHOR_IN_USE(HttpStatus.CONFLICT, "Author is still linked to books and cannot be deleted"),
    ISBN_ALREADY_EXISTS(HttpStatus.CONFLICT, "A book with the same ISBN already exists"),
    CONCURRENT_UPDATE(HttpStatus.CONFLICT, "The data was modified by another request, please retry"),
    // 409 vì yêu cầu hợp lệ về cú pháp nhưng xung đột với TRẠNG THÁI hiện tại của dữ liệu
    // (sách đã ngừng bán, tồn kho không đủ); trạng thái thay đổi thì yêu cầu có thể thành công.
    BOOK_NOT_AVAILABLE(HttpStatus.CONFLICT, "Book is no longer available for sale"),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "Not enough stock for the requested quantity"),
    CART_EMPTY(HttpStatus.CONFLICT, "Your cart is empty"),

    // ===== 404 / 405 / 409 / 415 =====
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "This book is not in your cart"),
    ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "Endpoint not found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported for this endpoint"),
    DATA_CONFLICT(HttpStatus.CONFLICT, "Request conflicts with existing data"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content type not supported"),

    // ===== 500 =====
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
