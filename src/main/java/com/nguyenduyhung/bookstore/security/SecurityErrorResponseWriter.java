package com.nguyenduyhung.bookstore.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import com.nguyenduyhung.bookstore.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Ghi response lỗi theo đúng format ApiResponse cho các lỗi xảy ra trong Security
 * Filter. Phải tự ghi tay vì những lỗi này xảy ra TRƯỚC DispatcherServlet, nơi
 * GlobalExceptionHandler không với tới được.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

    // Dùng ObjectMapper do Spring Boot cấu hình sẵn để Instant được ghi đúng định dạng ISO.
    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(),
                ApiResponse.error(errorCode, errorCode.getDefaultMessage(), null));
    }
}
