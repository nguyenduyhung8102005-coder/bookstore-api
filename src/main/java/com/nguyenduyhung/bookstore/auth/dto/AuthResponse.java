package com.nguyenduyhung.bookstore.auth.dto;

/**
 * @param expiresIn số GIÂY access token còn hiệu lực, để client biết khi nào cần refresh
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
    public static AuthResponse bearer(String accessToken, String refreshToken, long expiresIn) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
