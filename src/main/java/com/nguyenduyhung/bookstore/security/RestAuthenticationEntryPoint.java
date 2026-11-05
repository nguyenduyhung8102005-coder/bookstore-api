package com.nguyenduyhung.bookstore.security;

import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 401 Unauthorized: "Bạn là ai?". Được gọi khi request CHƯA đăng nhập (không có
 * token, hoặc token sai/hết hạn) mà lại truy cập endpoint cần đăng nhập.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorResponseWriter writer;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // Filter đã ghi lại lý do cụ thể (hết hạn / sai token) nếu có gửi token.
        Object reason = request.getAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE);
        ErrorCode errorCode = reason instanceof ErrorCode code ? code : ErrorCode.UNAUTHENTICATED;
        writer.write(response, errorCode);
    }
}
