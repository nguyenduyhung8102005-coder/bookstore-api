package com.nguyenduyhung.bookstore.user.dto;

import com.nguyenduyhung.bookstore.user.Role;

import java.time.Instant;

/**
 * Thông tin user trả ra ngoài. Cố ý KHÔNG có passwordHash và enabled.
 * Đây là lý do chính để không trả thẳng Entity: Entity chứa dữ liệu nhạy cảm.
 */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        Instant createdAt
) {
}
