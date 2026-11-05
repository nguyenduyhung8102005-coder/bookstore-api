package com.nguyenduyhung.bookstore.auth;

import com.nguyenduyhung.bookstore.auth.dto.AuthResponse;
import com.nguyenduyhung.bookstore.auth.dto.LoginRequest;
import com.nguyenduyhung.bookstore.auth.dto.RefreshTokenRequest;
import com.nguyenduyhung.bookstore.auth.dto.RegisterRequest;
import com.nguyenduyhung.bookstore.common.response.ApiResponse;
import com.nguyenduyhung.bookstore.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller chỉ làm 3 việc: nhận request, validate (@Valid), gọi Service và bọc
 * kết quả vào ApiResponse. Không có logic nghiệp vụ ở đây.
 */
@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Đăng ký tài khoản (role USER)")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request), "Registered successfully");
    }

    @Operation(summary = "Đăng nhập, nhận access token và refresh token")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @Operation(summary = "Đổi refresh token lấy cặp token mới (token cũ bị vô hiệu)")
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request.refreshToken()));
    }

    /**
     * Không yêu cầu access token: access token có thể đã hết hạn lúc người dùng
     * bấm đăng xuất. Giữ refresh token đã đủ chứng minh quyền thu hồi nó.
     */
    @Operation(summary = "Đăng xuất: thu hồi refresh token")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.success(null, "Logged out");
    }
}
