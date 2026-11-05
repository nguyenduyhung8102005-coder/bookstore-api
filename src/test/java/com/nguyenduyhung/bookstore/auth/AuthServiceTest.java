package com.nguyenduyhung.bookstore.auth;

import com.nguyenduyhung.bookstore.auth.dto.AuthResponse;
import com.nguyenduyhung.bookstore.auth.dto.LoginRequest;
import com.nguyenduyhung.bookstore.auth.dto.RegisterRequest;
import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import com.nguyenduyhung.bookstore.security.JwtProperties;
import com.nguyenduyhung.bookstore.security.JwtService;
import com.nguyenduyhung.bookstore.user.Role;
import com.nguyenduyhung.bookstore.user.User;
import com.nguyenduyhung.bookstore.user.UserRepository;
import com.nguyenduyhung.bookstore.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        var jwtProperties = new JwtProperties("unused", Duration.ofMinutes(15), Duration.ofDays(7));
        authService = new AuthService(userRepository, passwordEncoder, jwtService, refreshTokenService, jwtProperties);
    }

    @Test
    void register_normalizesEmail_hashesPassword_andAlwaysUsesRoleUser() {
        when(userRepository.existsByEmail("an@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret@123")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(
                new RegisterRequest("  An@Example.COM ", "Secret@123", " Nguyen An "));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("an@example.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(saved.getValue().getFullName()).isEqualTo("Nguyen An");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(response.email()).isEqualTo("an@example.com");
    }

    @Test
    void register_duplicateEmail_isRejected_andNothingIsSaved() {
        when(userRepository.existsByEmail("an@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("an@example.com", "Secret@123", "An")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_wrongPassword_isRejected() {
        User user = User.create("an@example.com", "bcrypt-hash", "An", Role.USER);
        when(userRepository.findByEmail("an@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "bcrypt-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("an@example.com", "wrong")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void login_unknownEmail_returnsSameErrorAsWrongPassword() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "whatever")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void login_disabledAccount_isRejected() {
        User user = User.create("an@example.com", "bcrypt-hash", "An", Role.USER);
        user.disable();
        when(userRepository.findByEmail("an@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret@123", "bcrypt-hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("an@example.com", "Secret@123")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_DISABLED);
    }

    @Test
    void login_success_returnsBothTokens() {
        User user = User.create("an@example.com", "bcrypt-hash", "An", Role.USER);
        when(userRepository.findByEmail("an@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret@123", "bcrypt-hash")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access");
        when(refreshTokenService.issue(user)).thenReturn("refresh");

        AuthResponse response = authService.login(new LoginRequest("AN@example.com", "Secret@123"));

        assertThat(response).isEqualTo(new AuthResponse("access", "refresh", "Bearer", 900));
    }
}
