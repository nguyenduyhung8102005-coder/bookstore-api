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
import com.nguyenduyhung.bookstore.user.UserMapper;
import com.nguyenduyhung.bookstore.user.UserRepository;
import com.nguyenduyhung.bookstore.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;

    /**
     * Đăng ký luôn tạo role USER; không cho client tự chọn role (nếu nhận role
     * từ request, ai cũng tự đăng ký được tài khoản ADMIN).
     *
     * Kiểm tra existsByEmail trước cho thông báo lỗi rõ ràng. Nếu 2 request trùng
     * email chạy song song, cả hai có thể cùng vượt qua bước này; khi đó ràng buộc
     * UNIQUE trong DB chặn request thứ hai (GlobalExceptionHandler trả 409).
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        User user = User.create(email, passwordEncoder.encode(request.password()), request.fullName(), Role.USER);
        return UserMapper.toResponse(userRepository.save(user));
    }

    /**
     * Sai email hay sai mật khẩu đều trả CHUNG một lỗi INVALID_CREDENTIALS, để
     * không giúp kẻ tấn công dò xem email nào đã có tài khoản.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(User.normalizeEmail(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));
        ensureEnabled(user);
        return issueTokens(user);
    }

    /** Xem giải thích noRollbackFor ở RefreshTokenService.consume(). */
    @Transactional(noRollbackFor = AppException.class)
    public AuthResponse refresh(String refreshToken) {
        User user = refreshTokenService.consume(refreshToken);
        ensureEnabled(user);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issue(user);
        return AuthResponse.bearer(accessToken, refreshToken, jwtProperties.accessTokenTtl().toSeconds());
    }

    private void ensureEnabled(User user) {
        if (!user.isEnabled()) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }
    }
}
