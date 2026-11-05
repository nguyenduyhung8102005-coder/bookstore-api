package com.nguyenduyhung.bookstore.security;

import com.nguyenduyhung.bookstore.user.Role;
import com.nguyenduyhung.bookstore.user.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // Hai khóa Base64 khác nhau, mỗi khóa 48 byte (> 256 bit)
    private static final String SECRET = "3/hUKy/YR7ZQmdE2Gv1RhXJJSl7z85wtueonrxDtl4dUMlEBUF5qbsBxAq2Joaiv";
    private static final String OTHER_SECRET = "q4n9Xb0yP2s7Lk1mVt5wZr8Hc3Jd6Fg0Ae9Bi2Cu4Ox7Ny1Mz5Pw8Qv3Rs6Tt0Uu";

    private static final Duration ACCESS_TTL = Duration.ofMinutes(15);
    private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");

    private final JwtService jwtService = serviceAt(SECRET, NOW);

    @Test
    void generatedToken_canBeParsedBack() {
        User user = user(42L, "an@example.com", Role.ADMIN);

        AuthUser authUser = jwtService.parseAccessToken(jwtService.generateAccessToken(user));

        assertThat(authUser).isEqualTo(new AuthUser(42L, "an@example.com", Role.ADMIN));
    }

    @Test
    void tokenIsStillValid_justBeforeExpiry() {
        String token = jwtService.generateAccessToken(user(1L, "a@example.com", Role.USER));
        JwtService justBeforeExpiry = serviceAt(SECRET, NOW.plus(ACCESS_TTL).minusSeconds(1));

        assertThat(justBeforeExpiry.parseAccessToken(token).id()).isEqualTo(1L);
    }

    @Test
    void expiredToken_isRejected() {
        String token = jwtService.generateAccessToken(user(1L, "a@example.com", Role.USER));
        // "Tua" đồng hồ tới sau thời điểm hết hạn 1 giây
        JwtService afterExpiry = serviceAt(SECRET, NOW.plus(ACCESS_TTL).plusSeconds(1));

        assertThatThrownBy(() -> afterExpiry.parseAccessToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenSignedWithAnotherKey_isRejected() {
        String forged = serviceAt(OTHER_SECRET, NOW)
                .generateAccessToken(user(1L, "hacker@example.com", Role.ADMIN));

        assertThatThrownBy(() -> jwtService.parseAccessToken(forged))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void garbageToken_isRejected() {
        assertThatThrownBy(() -> jwtService.parseAccessToken("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }

    private static JwtService serviceAt(String secret, Instant now) {
        var properties = new JwtProperties(secret, ACCESS_TTL, Duration.ofDays(7));
        return new JwtService(properties, Clock.fixed(now, ZoneOffset.UTC));
    }

    private static User user(Long id, String email, Role role) {
        User user = User.create(email, "hash", "Test User", role);
        // id bình thường do DB sinh; trong unit test không có DB nên gán bằng reflection.
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
