package com.nguyenduyhung.bookstore.auth;

import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import com.nguyenduyhung.bookstore.security.JwtProperties;
import com.nguyenduyhung.bookstore.user.Role;
import com.nguyenduyhung.bookstore.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test với Mockito: repository là "mock" (đối tượng giả), ta tự quy định nó
 * trả về gì (when...thenReturn) và kiểm tra service đã gọi nó ra sao (verify).
 * Không cần DB nên test chạy trong vài mili giây.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");
    private static final String RAW_TOKEN = "raw-refresh-token";

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        var properties = new JwtProperties("unused", Duration.ofMinutes(15), Duration.ofDays(7));
        service = new RefreshTokenService(repository, properties, Clock.fixed(NOW, ZoneOffset.UTC));

        user = User.create("an@example.com", "hash", "An", Role.USER);
        ReflectionTestUtils.setField(user, "id", 7L);
    }

    @Test
    void issue_storesOnlyTheHash_withCorrectExpiry() {
        String raw = service.issue(user);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash())
                .isNotEqualTo(raw)
                .isEqualTo(RefreshTokenService.hash(raw))
                .hasSize(64);
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
    }

    @Test
    void issue_generatesDifferentTokensEachTime() {
        assertThat(service.issue(user)).isNotEqualTo(service.issue(user));
    }

    @Test
    void consume_validToken_revokesItAndReturnsUser() {
        RefreshToken token = storedToken(NOW.plusSeconds(60), null);
        when(repository.findByTokenHashWithUser(RefreshTokenService.hash(RAW_TOKEN))).thenReturn(Optional.of(token));
        when(repository.revokeIfActive(1L, NOW)).thenReturn(1);

        assertThat(service.consume(RAW_TOKEN)).isSameAs(user);
        verify(repository).revokeIfActive(1L, NOW);
    }

    @Test
    void consume_unknownToken_isRejected() {
        when(repository.findByTokenHashWithUser(anyString())).thenReturn(Optional.empty());

        assertInvalidRefreshToken();
    }

    @Test
    void consume_expiredToken_isRejected() {
        RefreshToken token = storedToken(NOW, null); // hết hạn đúng thời điểm hiện tại
        when(repository.findByTokenHashWithUser(anyString())).thenReturn(Optional.of(token));

        assertInvalidRefreshToken();
        verify(repository, never()).revokeIfActive(any(Long.class), any(Instant.class));
    }

    @Test
    void consume_alreadyRevokedToken_revokesAllTokensOfUser() {
        RefreshToken token = storedToken(NOW.plusSeconds(60), NOW.minusSeconds(5));
        when(repository.findByTokenHashWithUser(anyString())).thenReturn(Optional.of(token));

        assertInvalidRefreshToken();
        verify(repository).revokeAllActiveByUserId(7L, NOW);
    }

    @Test
    void consume_lostRaceWithConcurrentRequest_isTreatedAsReuse() {
        RefreshToken token = storedToken(NOW.plusSeconds(60), null);
        when(repository.findByTokenHashWithUser(anyString())).thenReturn(Optional.of(token));
        when(repository.revokeIfActive(1L, NOW)).thenReturn(0); // request khác đã dùng trước

        assertInvalidRefreshToken();
        verify(repository).revokeAllActiveByUserId(7L, NOW);
    }

    private void assertInvalidRefreshToken() {
        assertThatThrownBy(() -> service.consume(RAW_TOKEN))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    private RefreshToken storedToken(Instant expiresAt, Instant revokedAt) {
        RefreshToken token = RefreshToken.create(user, RefreshTokenService.hash(RAW_TOKEN), expiresAt);
        ReflectionTestUtils.setField(token, "id", 1L);
        ReflectionTestUtils.setField(token, "revokedAt", revokedAt);
        return token;
    }
}
