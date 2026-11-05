package com.nguyenduyhung.bookstore.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Cung cấp "đồng hồ" dưới dạng bean thay vì gọi thẳng Instant.now().
 *
 * Lý do: code phụ thuộc thời gian (token hết hạn, đơn quá 30 phút...) rất khó
 * test nếu dùng giờ thật. Inject Clock thì trong test chỉ cần truyền
 * Clock.fixed(...) là "dừng" hoặc "tua" thời gian theo ý muốn.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
