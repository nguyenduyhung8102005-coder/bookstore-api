package com.nguyenduyhung.bookstore.user;

import com.nguyenduyhung.bookstore.user.dto.UserResponse;

/**
 * Chuyển Entity sang DTO, viết tay (không dùng MapStruct).
 *
 * Để là class tiện ích với static method vì việc chuyển đổi này không phụ thuộc
 * bean nào khác. Khi mapper cần gọi repository hay service, ta sẽ đổi thành @Component.
 */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
