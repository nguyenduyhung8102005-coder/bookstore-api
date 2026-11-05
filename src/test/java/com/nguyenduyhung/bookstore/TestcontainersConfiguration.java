package com.nguyenduyhung.bookstore;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Cấu hình dùng chung cho mọi integration test: khởi động một PostgreSQL THẬT
 * trong Docker (cùng version với docker-compose).
 *
 * @ServiceConnection: Spring Boot tự đọc host, port, username, password của
 * container và cấu hình DataSource, không cần ghi spring.datasource.* cho test.
 *
 * Vì sao không dùng H2 (DB trong bộ nhớ) cho nhanh? H2 khác PostgreSQL ở nhiều
 * chỗ (kiểu dữ liệu, hàm date_trunc, cú pháp khóa...). Test pass trên H2 chưa
 * chắc chạy đúng trên PostgreSQL, nên test trên đúng DB sẽ chạy production.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));
    }
}
