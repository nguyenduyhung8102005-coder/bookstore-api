package com.nguyenduyhung.bookstore.security;

import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Đọc header "Authorization: Bearer <token>" ở MỌI request.
 *
 * - Token hợp lệ: đặt AuthUser vào SecurityContext; các bước sau coi request là
 *   đã đăng nhập.
 * - Không có token hoặc token sai: KHÔNG chặn ở đây, chỉ để request đi tiếp với
 *   tư cách "ẩn danh". Nếu endpoint là public (GET /books) thì vẫn chạy bình
 *   thường; nếu endpoint cần đăng nhập, Spring Security sẽ gọi
 *   RestAuthenticationEntryPoint để trả 401.
 *
 * Cố ý KHÔNG đánh dấu @Component. Spring Boot tự đăng ký mọi bean kiểu Filter
 * vào chuỗi filter của servlet container; nếu là @Component, filter này sẽ chạy
 * cả ở ngoài Spring Security. Ta tạo nó bằng tay trong SecurityConfig.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Tên attribute để báo cho entry point biết vì sao token bị từ chối. */
    public static final String JWT_ERROR_ATTRIBUTE = "jwt.error";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                AuthUser user = jwtService.parseAccessToken(token);
                var authentication = UsernamePasswordAuthenticationToken.authenticated(
                        user, null, user.authorities());

                // Tạo context MỚI thay vì sửa context hiện có, theo khuyến nghị của
                // Spring Security, để tránh lỗi khi nhiều luồng dùng chung context.
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } catch (ExpiredJwtException e) {
                // Phân biệt "hết hạn" để client biết nên gọi /auth/refresh,
                // còn "token sai" thì phải đăng nhập lại.
                request.setAttribute(JWT_ERROR_ATTRIBUTE, ErrorCode.ACCESS_TOKEN_EXPIRED);
            } catch (JwtException e) {
                log.debug("Rejected access token: {}", e.getMessage());
                request.setAttribute(JWT_ERROR_ATTRIBUTE, ErrorCode.INVALID_ACCESS_TOKEN);
            }
        }

        filterChain.doFilter(request, response);
    }
}
