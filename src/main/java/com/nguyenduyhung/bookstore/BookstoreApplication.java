package com.nguyenduyhung.bookstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * - @ConfigurationPropertiesScan: tự đăng ký các record @ConfigurationProperties
 *   (JwtProperties, AdminProperties) trong package này.
 * - exclude UserDetailsServiceAutoConfiguration: mặc định Spring Boot tạo sẵn một
 *   user "user" với mật khẩu ngẫu nhiên in ra log. Ta tự xử lý đăng nhập bằng
 *   bảng users nên tắt cơ chế đó để tránh nhầm lẫn.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class BookstoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookstoreApplication.class, args);
    }
}
