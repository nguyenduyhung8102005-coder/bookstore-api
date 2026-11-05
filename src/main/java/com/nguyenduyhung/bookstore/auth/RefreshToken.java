package com.nguyenduyhung.bookstore.auth;

import com.nguyenduyhung.bookstore.common.entity.BaseEntity;
import com.nguyenduyhung.bookstore.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Một refresh token đã cấp cho user.
 *
 * DB chỉ lưu HASH của token. Nếu DB bị lộ, kẻ tấn công cũng không dùng được
 * các hash này để đăng nhập (giống nguyên tắc không lưu mật khẩu gốc).
 */
@Getter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    // LAZY: chỉ truy vấn bảng users khi thực sự gọi getUser().
    // Mặc định của @ManyToOne là EAGER, một nguồn gây N+1 hay gặp, nên luôn khai báo LAZY.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public static RefreshToken create(User user, String tokenHash, Instant expiresAt) {
        RefreshToken token = new RefreshToken();
        token.user = user;
        token.tokenHash = tokenHash;
        token.expiresAt = expiresAt;
        return token;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
