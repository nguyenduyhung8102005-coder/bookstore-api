package com.nguyenduyhung.bookstore.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bật cơ chế chạy định kỳ (@Scheduled).
 *
 * Tắt được bằng app.scheduling.enabled=false. Integration test tắt nó đi để job
 * không tự chạy chen ngang giữa các test; test gọi thẳng method của job khi cần,
 * nhờ vậy kết quả luôn xác định được.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
