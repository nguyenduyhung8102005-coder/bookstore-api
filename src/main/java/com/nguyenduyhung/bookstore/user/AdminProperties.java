package com.nguyenduyhung.bookstore.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Đọc nhóm cấu hình app.admin.* trong application.yml vào một record.
 * Được đăng ký nhờ @ConfigurationPropertiesScan trên BookstoreApplication.
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(String email, String password) {
}
