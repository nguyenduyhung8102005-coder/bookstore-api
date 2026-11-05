package com.nguyenduyhung.bookstore.order;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Cấu hình app.order.* trong application.yml.
 *
 * @param pendingTimeout đơn PENDING quá thời gian này mà chưa thanh toán thì bị
 *                       hủy và hoàn kho, để sách không bị "giam" mãi trong đơn bỏ dở
 */
@Validated
@ConfigurationProperties(prefix = "app.order")
public record OrderProperties(@NotNull Duration pendingTimeout) {
}
