package com.nguyenduyhung.bookstore.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Đọc nhóm cấu hình app.jwt.* thành một record có kiểu rõ ràng.
 * Spring Boot tự hiểu "15m", "7d" thành Duration.
 * @Validated: thiếu cấu hình thì app báo lỗi NGAY khi khởi động, không đợi tới lúc có request.
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotNull Duration accessTokenTtl,
        @NotNull Duration refreshTokenTtl
) {
}
