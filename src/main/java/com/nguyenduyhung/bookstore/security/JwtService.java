package com.nguyenduyhung.bookstore.security;

import com.nguyenduyhung.bookstore.user.Role;
import com.nguyenduyhung.bookstore.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Tạo và kiểm tra ACCESS token dạng JWT.
 *
 * JWT gồm 3 phần ngăn bởi dấu chấm: header.payload.signature
 * - header, payload: chỉ là JSON mã hóa Base64URL, AI CŨNG ĐỌC ĐƯỢC
 *   (dán vào jwt.io là thấy), nên tuyệt đối không bỏ mật khẩu hay dữ liệu nhạy cảm vào.
 * - signature: HMAC của header + payload với khóa bí mật. Sửa một ký tự trong
 *   payload (ví dụ đổi role thành ADMIN) là chữ ký không còn khớp, token bị từ chối.
 *
 * Thuật toán HMAC (đối xứng): cùng một khóa để ký và để kiểm tra. Phù hợp khi chỉ
 * một ứng dụng vừa phát hành vừa kiểm tra token. Khi nhiều service cùng cần kiểm
 * tra token, nên dùng RSA/EC (khóa riêng ký, khóa công khai kiểm tra).
 */
@Service
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey signingKey;
    private final Duration accessTokenTtl;
    private final Clock clock;
    private final JwtParser parser;

    public JwtService(JwtProperties properties, Clock clock) {
        // hmacShaKeyFor ném WeakKeyException nếu khóa ngắn hơn 256 bit,
        // nên cấu hình sai sẽ làm app dừng ngay khi khởi động.
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.accessTokenTtl = properties.accessTokenTtl();
        this.clock = clock;
        // JwtParser không đổi trạng thái sau khi build và an toàn khi dùng từ nhiều
        // luồng, nên tạo MỘT lần thay vì tạo lại ở mỗi request.
        this.parser = Jwts.parser()
                .verifyWith(signingKey)
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    /**
     * subject là id (không đổi), không phải email (có thể đổi sau này).
     * Kèm email và role để filter dựng AuthUser mà không cần truy vấn DB.
     */
    public String generateAccessToken(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Kiểm tra chữ ký và hạn dùng, rồi dựng AuthUser từ nội dung token.
     *
     * @throws io.jsonwebtoken.ExpiredJwtException token đã hết hạn
     * @throws JwtException token sai chữ ký, sai định dạng hoặc thiếu thông tin
     */
    public AuthUser parseAccessToken(String token) {
        Claims claims = parser.parseSignedClaims(token).getPayload();
        try {
            return new AuthUser(
                    Long.valueOf(requireClaim(claims.getSubject(), "sub")),
                    requireClaim(claims.get(CLAIM_EMAIL, String.class), CLAIM_EMAIL),
                    Role.valueOf(requireClaim(claims.get(CLAIM_ROLE, String.class), CLAIM_ROLE)));
        } catch (IllegalArgumentException e) {
            // subject không phải số, hoặc role không tồn tại trong enum
            throw new MalformedJwtException("Invalid claim value in token", e);
        }
    }

    private static String requireClaim(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new MalformedJwtException("Missing claim: " + name);
        }
        return value;
    }
}
