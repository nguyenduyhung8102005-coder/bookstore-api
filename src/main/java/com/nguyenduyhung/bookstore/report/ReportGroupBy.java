package com.nguyenduyhung.bookstore.report;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Cách gom doanh thu: theo ngày hoặc theo tháng.
 *
 * Mỗi giá trị biết: đơn vị tương ứng của hàm date_trunc trong PostgreSQL, ngày bắt
 * đầu của kỳ chứa một ngày bất kỳ, kỳ kế tiếp, và nhãn hiển thị. Dùng switch
 * expression để thêm giá trị mới (WEEK, QUARTER...) thì trình biên dịch bắt buộc
 * xử lý đủ mọi chỗ.
 */
public enum ReportGroupBy {
    DAY,
    MONTH;

    /** Tham số cho date_trunc(...). Lấy từ enum nên client không thể chèn chuỗi tùy ý. */
    String sqlUnit() {
        return switch (this) {
            case DAY -> "day";
            case MONTH -> "month";
        };
    }

    LocalDate periodStart(LocalDate date) {
        return switch (this) {
            case DAY -> date;
            case MONTH -> date.withDayOfMonth(1);
        };
    }

    LocalDate nextPeriod(LocalDate periodStart) {
        return switch (this) {
            case DAY -> periodStart.plusDays(1);
            case MONTH -> periodStart.plusMonths(1);
        };
    }

    /** Ví dụ "2026-11-05" (theo ngày) hoặc "2026-11" (theo tháng). */
    String label(LocalDate periodStart) {
        return switch (this) {
            case DAY -> periodStart.toString();
            case MONTH -> YearMonth.from(periodStart).toString();
        };
    }
}
