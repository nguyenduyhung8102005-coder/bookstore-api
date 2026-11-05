package com.nguyenduyhung.bookstore.security;

import com.nguyenduyhung.bookstore.user.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * Người dùng đã xác thực của request hiện tại, dựng lại từ nội dung access token.
 * Controller lấy ra bằng @AuthenticationPrincipal AuthUser currentUser.
 */
public record AuthUser(Long id, String email, Role role) {

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }
}
