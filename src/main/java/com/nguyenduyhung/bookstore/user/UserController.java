package com.nguyenduyhung.bookstore.user;

import com.nguyenduyhung.bookstore.common.config.OpenApiConfig;
import com.nguyenduyhung.bookstore.common.response.ApiResponse;
import com.nguyenduyhung.bookstore.security.AuthUser;
import com.nguyenduyhung.bookstore.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * @AuthenticationPrincipal lấy "principal" mà JwtAuthenticationFilter đã đặt
     * vào SecurityContext. Id user luôn lấy từ token đã xác thực, không bao giờ
     * nhận từ URL hay body, nên user không thể xem thông tin của người khác.
     */
    @Operation(summary = "Thông tin người dùng đang đăng nhập")
    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal AuthUser currentUser) {
        return ApiResponse.success(userService.getById(currentUser.id()));
    }
}
