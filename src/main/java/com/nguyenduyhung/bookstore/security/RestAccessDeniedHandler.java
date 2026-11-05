package com.nguyenduyhung.bookstore.security;

import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 403 Forbidden: "Tôi biết bạn là ai, nhưng bạn không có quyền". Ví dụ USER gọi
 * endpoint /api/v1/admin/**.
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorResponseWriter writer;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        writer.write(response, ErrorCode.ACCESS_DENIED);
    }
}
