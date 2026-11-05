package com.nguyenduyhung.bookstore.user;

/**
 * Vai trò của người dùng. Lưu trong DB dưới dạng chuỗi ('USER', 'ADMIN').
 *
 * Spring Security quy ước quyền dạng role phải có tiền tố "ROLE_"
 * (ví dụ "ROLE_ADMIN"); hasRole("ADMIN") tự thêm tiền tố đó khi so sánh.
 */
public enum Role {
    USER,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
