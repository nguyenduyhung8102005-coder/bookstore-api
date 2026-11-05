package com.nguyenduyhung.bookstore.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * "join fetch" lấy luôn User trong cùng 1 câu SQL, vì gần như lần nào tìm
     * token xong cũng cần đến user (tránh thêm 1 query lazy-load sau đó).
     */
    @Query("select t from RefreshToken t join fetch t.user where t.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashWithUser(@Param("tokenHash") String tokenHash);

    /**
     * Thu hồi token NẾU nó còn hiệu lực, trong MỘT câu UPDATE duy nhất.
     *
     * Vì sao không đọc token, gọi setRevokedAt rồi save? Nếu 2 request cùng dùng
     * một refresh token đến gần như cùng lúc, cả hai cùng đọc thấy "chưa thu hồi"
     * và cùng được cấp token mới. Câu UPDATE có điều kiện "revoked_at is null" là
     * nguyên tử: chỉ một request đổi được dòng đó (trả về 1), request còn lại nhận 0.
     *
     * Lưu ý: UPDATE hàng loạt đi thẳng xuống DB, bỏ qua JPA Auditing, nên phải tự set updatedAt.
     */
    @Modifying
    @Query("""
            update RefreshToken t
               set t.revokedAt = :now, t.updatedAt = :now
             where t.id = :id and t.revokedAt is null
            """)
    int revokeIfActive(@Param("id") Long id, @Param("now") Instant now);

    /** Thu hồi toàn bộ token còn hiệu lực của một user (khi phát hiện token bị dùng lại). */
    @Modifying
    @Query("""
            update RefreshToken t
               set t.revokedAt = :now, t.updatedAt = :now
             where t.user.id = :userId and t.revokedAt is null
            """)
    int revokeAllActiveByUserId(@Param("userId") Long userId, @Param("now") Instant now);
}
