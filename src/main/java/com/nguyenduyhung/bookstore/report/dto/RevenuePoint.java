package com.nguyenduyhung.bookstore.report.dto;

import java.math.BigDecimal;

/**
 * Doanh thu của một kỳ.
 *
 * @param period nhãn kỳ: "2026-11-05" (theo ngày) hoặc "2026-11" (theo tháng)
 */
public record RevenuePoint(String period, BigDecimal revenue, long orderCount) {
}
