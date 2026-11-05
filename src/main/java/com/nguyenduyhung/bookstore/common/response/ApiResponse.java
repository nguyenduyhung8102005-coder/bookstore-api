package com.nguyenduyhung.bookstore.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;

import java.time.Instant;
import java.util.List;

/**
 * Khuôn response chung cho MỌI endpoint, thành công hay lỗi.
 *
 * Client chỉ cần một cách đọc duy nhất: kiểm tra `success`, lấy `data` khi
 * thành công, hoặc đọc `code` + `errors` khi lỗi.
 *
 * @JsonInclude(NON_NULL): field nào null thì không xuất hiện trong JSON,
 * nên response thành công không có "errors", response lỗi không có "data".
 *
 * @param <T> kiểu dữ liệu trả về trong `data`
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        List<FieldErrorResponse> errors,
        Instant timestamp
) {

    public static final String SUCCESS_CODE = "SUCCESS";

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "OK");
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, SUCCESS_CODE, message, data, null, Instant.now());
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message, List<FieldErrorResponse> errors) {
        return new ApiResponse<>(false, errorCode.name(), message, null, errors, Instant.now());
    }
}
