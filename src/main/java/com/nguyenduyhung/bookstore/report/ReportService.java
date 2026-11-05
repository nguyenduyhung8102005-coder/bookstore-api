package com.nguyenduyhung.bookstore.report;

import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import com.nguyenduyhung.bookstore.order.OrderStatus;
import com.nguyenduyhung.bookstore.report.dto.BestSellerResponse;
import com.nguyenduyhung.bookstore.report.dto.RevenuePoint;
import com.nguyenduyhung.bookstore.report.dto.RevenueReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Thống kê cho admin.
 *
 * Phần nặng (lọc, cộng, gom nhóm) làm trong DB: DB chỉ trả về vài chục dòng đã
 * tổng hợp thay vì hàng nghìn đơn hàng. Service chỉ lo: kiểm tra tham số, đổi
 * ngày (giờ Việt Nam) sang mốc thời gian UTC, và điền 0 cho kỳ không có đơn.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    /** Báo cáo theo ngày tối đa 366 ngày (tức tối đa 366 điểm trên biểu đồ). */
    static final int MAX_DAILY_RANGE_DAYS = 366;
    /** Mọi báo cáo khác (theo tháng, sách bán chạy) tối đa 36 tháng, để không quét cả bảng orders. */
    static final int MAX_RANGE_MONTHS = 36;

    /** Tên các trạng thái tính vào doanh thu, suy ra từ OrderStatus để luật chỉ nằm một chỗ. */
    static final List<String> REVENUE_STATUSES = Stream.of(OrderStatus.values())
            .filter(OrderStatus::countsAsRevenue)
            .map(Enum::name)
            .toList();

    private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(2);

    private final ReportRepository reportRepository;
    private final ReportProperties reportProperties;

    public RevenueReportResponse getRevenue(LocalDate from, LocalDate to, ReportGroupBy groupBy) {
        validateRange(from, to, groupBy == ReportGroupBy.DAY);
        ZoneId zone = reportProperties.zoneId();

        List<ReportRepository.RevenueRow> rows = reportRepository.sumRevenueByPeriod(
                groupBy.sqlUnit(), zone.getId(), startOf(from, zone), endOf(to, zone), REVENUE_STATUSES);

        List<RevenuePoint> points = fillMissingPeriods(rows, from, to, groupBy);
        BigDecimal totalRevenue = points.stream().map(RevenuePoint::revenue).reduce(ZERO_MONEY, BigDecimal::add);
        long totalOrders = points.stream().mapToLong(RevenuePoint::orderCount).sum();

        return new RevenueReportResponse(from, to, groupBy, zone.getId(), totalRevenue, totalOrders, points);
    }

    public List<BestSellerResponse> getBestSellers(LocalDate from, LocalDate to, int limit) {
        validateRange(from, to, false);
        ZoneId zone = reportProperties.zoneId();

        List<ReportRepository.BestSellerRow> rows = reportRepository.findBestSellers(
                startOf(from, zone), endOf(to, zone), REVENUE_STATUSES, limit);

        // Thứ hạng = vị trí trong danh sách (DB đã sắp xếp), bắt đầu từ 1.
        return IntStream.range(0, rows.size())
                .mapToObj(i -> {
                    ReportRepository.BestSellerRow row = rows.get(i);
                    return new BestSellerResponse(i + 1, row.getBookId(), row.getTitle(),
                            row.getQuantitySold(), row.getRevenue());
                })
                .toList();
    }

    // ===================== Helper =====================

    private static void validateRange(LocalDate from, LocalDate to, boolean daily) {
        if (from.isAfter(to)) {
            throw new AppException(ErrorCode.INVALID_PARAMETER, "'from' must be on or before 'to'");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (daily && days > MAX_DAILY_RANGE_DAYS) {
            throw new AppException(ErrorCode.INVALID_PARAMETER,
                    "Daily reports cover at most %d days; use groupBy=MONTH for longer ranges"
                            .formatted(MAX_DAILY_RANGE_DAYS));
        }
        if (!to.isBefore(from.plusMonths(MAX_RANGE_MONTHS))) {
            throw new AppException(ErrorCode.INVALID_PARAMETER,
                    "Reports cover at most %d months".formatted(MAX_RANGE_MONTHS));
        }
    }

    /** 00:00 ngày from theo giờ Việt Nam, đổi ra mốc UTC. Ví dụ 2026-11-05 → 2026-11-04T17:00:00Z. */
    private static Instant startOf(LocalDate from, ZoneId zone) {
        return from.atStartOfDay(zone).toInstant();
    }

    /**
     * 00:00 của NGÀY SAU ngày to (mốc loại trừ). Dùng "nhỏ hơn 00:00 hôm sau" thay vì
     * "nhỏ hơn hoặc bằng 23:59:59" để không bỏ sót đơn trả tiền lúc 23:59:59.5.
     */
    private static Instant endOf(LocalDate to, ZoneId zone) {
        return to.plusDays(1).atStartOfDay(zone).toInstant();
    }

    /**
     * DB chỉ trả về kỳ có đơn. Ở đây đi lần lượt từng kỳ trong [from, to] và điền 0
     * cho kỳ trống. Kỳ đầu/cuối của báo cáo theo tháng có thể chỉ là một phần tháng
     * (ví dụ from = 15/01 thì kỳ "2026-01" chỉ tính từ ngày 15).
     */
    private static List<RevenuePoint> fillMissingPeriods(List<ReportRepository.RevenueRow> rows,
                                                         LocalDate from, LocalDate to, ReportGroupBy groupBy) {
        Map<LocalDate, ReportRepository.RevenueRow> rowsByPeriod = rows.stream()
                .collect(Collectors.toMap(row -> LocalDate.parse(row.getPeriod()), Function.identity()));

        List<RevenuePoint> points = new ArrayList<>();
        for (LocalDate period = groupBy.periodStart(from); !period.isAfter(to); period = groupBy.nextPeriod(period)) {
            ReportRepository.RevenueRow row = rowsByPeriod.get(period);
            points.add(row == null
                    ? new RevenuePoint(groupBy.label(period), ZERO_MONEY, 0)
                    : new RevenuePoint(groupBy.label(period), row.getRevenue(), row.getOrderCount()));
        }
        return points;
    }
}
