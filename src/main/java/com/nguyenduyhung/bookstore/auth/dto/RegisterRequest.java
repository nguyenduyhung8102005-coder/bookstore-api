package com.nguyenduyhung.bookstore.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Giới hạn mật khẩu tối đa 72: BCrypt chỉ dùng 72 BYTE đầu tiên, phần dư bị bỏ
 * qua âm thầm (hai mật khẩu chỉ khác nhau sau byte 72 sẽ được coi là giống nhau).
 */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 255)
        String email,

        @NotBlank @Size(min = 8, max = 72)
        String password,

        @NotBlank @Size(max = 100)
        String fullName
) {
}
