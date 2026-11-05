package com.nguyenduyhung.bookstore.common.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình thông tin chung cho tài liệu Swagger.
 *
 * Khai báo sẵn security scheme "bearerAuth" để từ Giai đoạn 1, các endpoint cần
 * đăng nhập chỉ cần gắn @SecurityRequirement(name = "bearerAuth") là Swagger UI
 * hiện nút "Authorize" cho phép dán JWT vào để thử.
 */
@Configuration
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI bookstoreOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Bookstore API")
                .version("v1")
                .description("REST API cho hệ thống bán sách trực tuyến")
                .contact(new Contact().name("Nguyen Duy Hung")));
    }
}
