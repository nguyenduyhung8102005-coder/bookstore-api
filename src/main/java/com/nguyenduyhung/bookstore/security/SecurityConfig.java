package com.nguyenduyhung.bookstore.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cấu hình Spring Security cho REST API dùng JWT.
 *
 * Mỗi request đi qua một chuỗi filter (Security Filter Chain) trước khi tới Controller:
 *   JwtAuthenticationFilter (xác định "bạn là ai")
 *   -> AuthorizationFilter (kiểm tra luật trong authorizeHttpRequests bên dưới)
 *   -> nếu không đạt: entry point (401) hoặc access denied handler (403)
 *   -> nếu đạt: DispatcherServlet -> Controller
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // bật @PreAuthorize trên method, sẽ dùng từ các giai đoạn sau
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_DOCS = {
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health/**"
    };

    private final JwtService jwtService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF tấn công bằng cách lợi dụng cookie trình duyệt TỰ ĐỘNG gửi kèm.
                // API này xác thực bằng header Authorization do client chủ động gắn,
                // trình duyệt không tự gửi, nên CSRF không áp dụng và có thể tắt.
                .csrf(AbstractHttpConfigurer::disable)
                // Không tạo HttpSession: mọi thông tin xác thực nằm trong token của từng request.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Tắt form login và HTTP Basic mặc định (trang đăng nhập HTML, popup trình duyệt).
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // Luật được xét TỪ TRÊN XUỐNG, luật đầu tiên khớp sẽ được áp dụng,
                // nên luật cụ thể phải đứng trước, anyRequest() luôn ở cuối.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/books/**", "/api/v1/categories/**", "/api/v1/authors/**").permitAll()
                        .requestMatchers(PUBLIC_DOCS).permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // "/error" là nơi Spring Boot chuyển tới khi có lỗi chưa xử lý;
                        // nếu chặn, client sẽ nhận 401 thay vì lỗi thật.
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt: tự sinh salt ngẫu nhiên cho từng mật khẩu (lưu ngay trong chuỗi hash)
     * và cố tình chậm (cost factor mặc định 10 = 2^10 vòng), khiến việc dò mật khẩu
     * hàng loạt tốn kém hơn rất nhiều so với MD5/SHA.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
