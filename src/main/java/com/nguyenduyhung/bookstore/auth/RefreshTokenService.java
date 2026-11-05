package com.nguyenduyhung.bookstore.auth;

import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import com.nguyenduyhung.bookstore.security.JwtProperties;
import com.nguyenduyhung.bookstore.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Quản lý vòng đời refresh token: cấp, dùng (kèm xoay vòng) và thu hồi.
 *
 * Refresh token ở đây là một chuỗi NGẪU NHIÊN (opaque), không phải JWT. Vì ta
 * luôn phải tra DB để biết token đã bị thu hồi chưa, nên không cần token tự chứa
 * thông tin như JWT; chuỗi ngẫu nhiên đơn giản hơn và không lộ dữ liệu gì.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32; // 256 bit ngẫu nhiên
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    /**
     * Cấp refresh token mới cho user.
     *
     * @return token GỐC, chỉ trả cho client đúng một lần. DB chỉ giữ hash của nó.
     */
    @Transactional
    public String issue(User user) {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Instant expiresAt = clock.instant().plus(jwtProperties.refreshTokenTtl());
        refreshTokenRepository.save(RefreshToken.create(user, hash(rawToken), expiresAt));
        return rawToken;
    }

    /**
     * Dùng refresh token để đổi lấy token mới (xoay vòng - rotation).
     * Token đã dùng sẽ bị thu hồi ngay; mỗi refresh token chỉ dùng được MỘT lần.
     *
     * Phát hiện dùng lại (reuse detection): nếu một token ĐÃ bị thu hồi lại được
     * gửi lên, nghĩa là có 2 bên đang giữ cùng một token, rất có thể token bị
     * đánh cắp. Khi đó thu hồi TOÀN BỘ token của user, buộc đăng nhập lại.
     *
     * noRollbackFor: mặc định Spring rollback khi có RuntimeException. Ở đây ta
     * vừa thu hồi toàn bộ token rồi mới ném lỗi; nếu rollback thì việc thu hồi
     * cũng bị hủy. Nên khai báo: gặp AppException thì vẫn COMMIT.
     *
     * @return user sở hữu token, để bên gọi cấp cặp token mới
     */
    @Transactional(noRollbackFor = AppException.class)
    public User consume(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHashWithUser(hash(rawToken))
                .orElseThrow(RefreshTokenService::invalidToken);
        Instant now = clock.instant();

        if (token.isRevoked()) {
            revokeAllBecauseOfReuse(token, now);
            throw invalidToken();
        }
        if (token.isExpired(now)) {
            throw invalidToken();
        }
        // 0 nghĩa là một request khác vừa dùng token này trước ta vài mili giây.
        if (refreshTokenRepository.revokeIfActive(token.getId(), now) == 0) {
            revokeAllBecauseOfReuse(token, now);
            throw invalidToken();
        }
        return token.getUser();
    }

    /**
     * Thu hồi một token (đăng xuất). Luôn "thành công" kể cả khi token không tồn
     * tại, để không tiết lộ cho người gọi biết token nào có thật.
     */
    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHashWithUser(hash(rawToken))
                .ifPresent(token -> refreshTokenRepository.revokeIfActive(token.getId(), clock.instant()));
    }

    private void revokeAllBecauseOfReuse(RefreshToken token, Instant now) {
        Long userId = token.getUser().getId();
        int revoked = refreshTokenRepository.revokeAllActiveByUserId(userId, now);
        log.warn("Refresh token reuse detected for user {}: revoked {} active token(s)", userId, revoked);
    }

    /**
     * SHA-256 thay vì BCrypt: BCrypt cố tình chậm và có salt ngẫu nhiên, hợp với
     * mật khẩu (ngắn, dễ đoán) nhưng không tra cứu được theo hash. Token ở đây là
     * 256 bit ngẫu nhiên nên không thể dò ngược; một hash nhanh, cố định là đủ an
     * toàn và cho phép tìm token bằng điều kiện WHERE token_hash = ?.
     */
    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // Mọi JVM đều bắt buộc hỗ trợ SHA-256, nên nhánh này coi như không xảy ra.
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static AppException invalidToken() {
        return new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
    }
}
