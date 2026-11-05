package com.nguyenduyhung.bookstore.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Bật JPA Auditing để tự điền createdAt / updatedAt cho các entity kế thừa BaseEntity.
 *
 * Vì sao để ở class riêng thay vì đặt @EnableJpaAuditing lên BookstoreApplication?
 * Các test "slice" như @WebMvcTest vẫn nạp class main nhưng KHÔNG nạp tầng JPA.
 * Nếu annotation nằm trên class main, test đó sẽ lỗi "JPA metamodel must not be empty".
 * Tách ra class @Configuration riêng thì @WebMvcTest bỏ qua nó.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
