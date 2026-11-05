package com.nguyenduyhung.bookstore.common.exception;

import lombok.Getter;

/**
 * Exception nghiệp vụ duy nhất của ứng dụng.
 *
 * Service ném AppException kèm một ErrorCode; GlobalExceptionHandler bắt lại và
 * chuyển thành response lỗi chuẩn. Nhờ vậy Service không cần biết gì về HTTP.
 *
 * Kế thừa RuntimeException (unchecked) để không phải khai báo `throws` khắp nơi,
 * và để @Transactional tự rollback (mặc định Spring chỉ rollback với unchecked exception).
 */
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage());
    }

    public AppException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** Lối tắt cho lỗi hay gặp nhất, ví dụ: throw AppException.notFound("Book", id); */
    public static AppException notFound(String resourceName, Object id) {
        return new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                "%s not found with id: %s".formatted(resourceName, id));
    }
}
