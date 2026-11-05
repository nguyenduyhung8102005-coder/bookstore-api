package com.nguyenduyhung.bookstore.report.dto;

import com.nguyenduyhung.bookstore.report.ReportGroupBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Báo cáo doanh thu. points có ĐỦ mọi kỳ trong khoảng [from, to], kể cả kỳ không có
 * đơn (doanh thu 0), để frontend vẽ biểu đồ không bị "mất" ngày.
 */
public record RevenueReportResponse(
        LocalDate from,
        LocalDate to,
        ReportGroupBy groupBy,
        String timeZone,
        BigDecimal totalRevenue,
        long totalOrders,
        List<RevenuePoint> points) {
}
