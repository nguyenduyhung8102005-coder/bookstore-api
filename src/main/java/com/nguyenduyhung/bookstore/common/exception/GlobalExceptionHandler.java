package com.nguyenduyhung.bookstore.common.exception;

import com.nguyenduyhung.bookstore.common.response.ApiResponse;
import com.nguyenduyhung.bookstore.common.response.FieldErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Bắt mọi exception thoát ra khỏi Controller và chuyển thành ApiResponse lỗi chuẩn.
 *
 * Nguyên tắc:
 * - Lỗi 4xx (do client): trả thông báo đủ rõ để client tự sửa request.
 * - Lỗi 5xx (do server): ghi log đầy đủ stack trace, nhưng chỉ trả câu chung chung
 *   cho client. Không bao giờ để lộ tên bảng, câu SQL hay stack trace ra ngoài.
 *
 * Giới hạn cần nhớ: class này chỉ bắt được exception xảy ra BÊN TRONG DispatcherServlet
 * (Controller và các tầng nó gọi). Lỗi 401/403 sinh ra ở Security Filter chạy TRƯỚC
 * DispatcherServlet nên không tới được đây; chúng được xử lý ở
 * security/RestAuthenticationEntryPoint và security/RestAccessDeniedHandler.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===================== Lỗi nghiệp vụ =====================

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        return build(ex.getErrorCode(), ex.getMessage(), null);
    }

    // ===================== Lỗi validation =====================

    /** @Valid trên @RequestBody không hợp lệ, ví dụ email để trống. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleBodyValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorResponse> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorResponse(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), errors);
    }

    /**
     * Ràng buộc đặt trực tiếp trên tham số của Controller, ví dụ
     * {@code @RequestParam @Max(100) int size}. Từ Spring 6.1, Spring MVC tự
     * validate các tham số này và ném HandlerMethodValidationException.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleParameterValidation(HandlerMethodValidationException ex) {
        List<FieldErrorResponse> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldErrorResponse(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), errors);
    }

    /** Validation ở tầng Service (class gắn @Validated) ném ra exception này. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldErrorResponse> errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldErrorResponse(lastNodeName(v), v.getMessage()))
                .toList();
        return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), errors);
    }

    // ===================== Request sai định dạng =====================

    /** JSON hỏng cú pháp, hoặc sai kiểu, ví dụ gửi "abc" cho field số. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return build(ErrorCode.INVALID_REQUEST_BODY, ErrorCode.INVALID_REQUEST_BODY.getDefaultMessage(), null);
    }

    /** Ví dụ GET /books/abc trong khi id phải là số. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Parameter '%s' has invalid value '%s'".formatted(ex.getName(), ex.getValue());
        return build(ErrorCode.INVALID_PARAMETER, message, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        String message = "Required parameter '%s' is missing".formatted(ex.getParameterName());
        return build(ErrorCode.INVALID_PARAMETER, message, null);
    }

    // ===================== Lỗi định tuyến =====================

    /**
     * URL không khớp endpoint nào. Từ Spring 6.1, trường hợp này đi qua bộ xử lý
     * static resource và ném NoResourceFoundException; NoHandlerFoundException
     * giữ lại cho cấu hình kiểu cũ.
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFoundEndpoint(Exception ex) {
        return build(ErrorCode.ENDPOINT_NOT_FOUND, ErrorCode.ENDPOINT_NOT_FOUND.getDefaultMessage(), null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String message = "Method '%s' is not supported for this endpoint".formatted(ex.getMethod());
        return build(ErrorCode.METHOD_NOT_ALLOWED, message, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return build(ErrorCode.UNSUPPORTED_MEDIA_TYPE, ErrorCode.UNSUPPORTED_MEDIA_TYPE.getDefaultMessage(), null);
    }

    // ===================== Phân quyền ở tầng method =====================

    /**
     * Luật phân quyền theo URL (trong SecurityConfig) bị chặn ở Security Filter và
     * đi qua RestAccessDeniedHandler. Còn @PreAuthorize trên method thì chạy BÊN
     * TRONG DispatcherServlet, nên exception rơi vào đây. Không có handler này,
     * nó sẽ bị handleUnexpected() bắt và trả nhầm 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return build(ErrorCode.ACCESS_DENIED, ErrorCode.ACCESS_DENIED.getDefaultMessage(), null);
    }

    // ===================== Lỗi dữ liệu =====================

    /**
     * Lưới an toàn cuối cùng cho ràng buộc DB (UNIQUE, FK, CHECK).
     * Service vẫn phải tự kiểm tra trước (ví dụ email đã tồn tại chưa), nhưng khi
     * 2 request chạy song song, cả hai có thể cùng vượt qua bước kiểm tra đó;
     * lúc ấy chỉ ràng buộc trong DB mới chặn được, và exception rơi vào đây.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return build(ErrorCode.DATA_CONFLICT, ErrorCode.DATA_CONFLICT.getDefaultMessage(), null);
    }

    // ===================== Lỗi không lường trước =====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage(), null);
    }

    // ===================== Helper =====================

    private ResponseEntity<ApiResponse<Void>> build(ErrorCode errorCode, String message,
                                                    List<FieldErrorResponse> errors) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode, message, errors));
    }

    /** "createOrder.request.quantity" -> "quantity" */
    private String lastNodeName(ConstraintViolation<?> violation) {
        String name = null;
        for (Path.Node node : violation.getPropertyPath()) {
            name = node.getName();
        }
        return name;
    }
}
