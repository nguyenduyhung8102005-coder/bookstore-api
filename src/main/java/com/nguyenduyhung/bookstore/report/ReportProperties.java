package com.nguyenduyhung.bookstore.report;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;

/**
 * Cấu hình app.report.* trong application.yml.
 *
 * @param timeZone múi giờ dùng để chia "một ngày" khi thống kê (mặc định Asia/Ho_Chi_Minh).
 *                 DB lưu thời điểm theo UTC; nếu gom theo ngày UTC thì đơn trả tiền lúc
 *                 0h30 sáng giờ Việt Nam sẽ bị tính vào ngày hôm trước.
 */
@Validated
@ConfigurationProperties(prefix = "app.report")
public record ReportProperties(@NotBlank String timeZone) {

    public ReportProperties {
        // Gọi thử ZoneId.of ngay lúc khởi động: tên múi giờ sai thì app dừng luôn,
        // thay vì đến lúc admin mở báo cáo mới lỗi 500.
        if (timeZone != null && !timeZone.isBlank()) {
            ZoneId.of(timeZone);
        }
    }

    public ZoneId zoneId() {
        return ZoneId.of(timeZone);
    }
}
