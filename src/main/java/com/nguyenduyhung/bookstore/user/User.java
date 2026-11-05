package com.nguyenduyhung.bookstore.user;

import com.nguyenduyhung.bookstore.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Locale;

/**
 * Người dùng hệ thống.
 *
 * - Tên bảng là "users" vì "user" là từ khóa dành riêng trong PostgreSQL.
 * - Constructor không tham số để PROTECTED: JPA bắt buộc phải có, nhưng code
 *   nghiệp vụ không nên tạo một User "rỗng"; muốn tạo thì đi qua User.create().
 * - Không có setter tự do: trạng thái chỉ đổi qua method có tên nghiệp vụ
 *   (disable()...), giúp đọc code là hiểu ngay ý định.
 */
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    // STRING thay vì ORDINAL: ORDINAL lưu số thứ tự (0, 1...), chỉ cần chèn
    // thêm một giá trị vào giữa enum là toàn bộ dữ liệu cũ bị hiểu sai.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    public static User create(String email, String passwordHash, String fullName, Role role) {
        User user = new User();
        user.email = normalizeEmail(email);
        user.passwordHash = passwordHash;
        user.fullName = fullName.trim();
        user.role = role;
        return user;
    }

    public void disable() {
        this.enabled = false;
    }

    /**
     * Email không phân biệt hoa thường: "An@Mail.com" và "an@mail.com" là một.
     * Mọi chỗ lưu hoặc tìm theo email đều phải đi qua hàm này để nhất quán.
     * Locale.ROOT tránh lỗi đổi chữ hoa/thường theo ngôn ngữ máy (ví dụ tiếng Thổ Nhĩ Kỳ).
     */
    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
