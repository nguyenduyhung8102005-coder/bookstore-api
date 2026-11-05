package com.nguyenduyhung.bookstore.report;

import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import com.nguyenduyhung.bookstore.report.dto.BestSellerResponse;
import com.nguyenduyhung.bookstore.report.dto.RevenuePoint;
import com.nguyenduyhung.bookstore.report.dto.RevenueReportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final String ZONE = "Asia/Ho_Chi_Minh";

    @Mock
    private ReportRepository reportRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        // ReportProperties là record (final): tạo thật thay vì mock.
        reportService = new ReportService(reportRepository, new ReportProperties(ZONE));
    }

    @Test
    void revenueStatuses_arePaidShippingAndCompleted() {
        assertThat(ReportService.REVENUE_STATUSES).containsExactly("PAID", "SHIPPING", "COMPLETED");
    }

    // ===================== Doanh thu =====================

    /**
     * Stub với ĐÚNG giá trị tham số: nếu Service tính sai mốc giờ, Mockito (strict stubs)
     * báo lỗi "PotentialStubbingProblem" vì lời gọi thật không khớp với stub.
     */
    @Test
    void daily_convertsVietnamDatesToUtcBoundaries_andFillsEmptyDays() {
        when(reportRepository.sumRevenueByPeriod("day", ZONE,
                Instant.parse("2026-10-31T17:00:00Z"),   // 00:00 ngày 01/11 giờ Việt Nam
                Instant.parse("2026-11-03T17:00:00Z"),   // 00:00 ngày 04/11 giờ Việt Nam (loại trừ)
                ReportService.REVENUE_STATUSES))
                .thenReturn(List.of(revenueRow("2026-11-02", "150000.00", 2)));

        RevenueReportResponse report = reportService.getRevenue(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), ReportGroupBy.DAY);

        assertThat(report.points()).extracting(RevenuePoint::period)
                .containsExactly("2026-11-01", "2026-11-02", "2026-11-03");
        assertThat(report.points()).extracting(RevenuePoint::orderCount).containsExactly(0L, 2L, 0L);
        assertThat(report.points().get(0).revenue()).isEqualByComparingTo("0");
        assertThat(report.totalRevenue()).isEqualByComparingTo("150000");
        assertThat(report.totalOrders()).isEqualTo(2);
        assertThat(report.timeZone()).isEqualTo(ZONE);
    }

    @Test
    void monthly_labelsByMonth_andFirstPeriodIsMonthContainingFrom() {
        when(reportRepository.sumRevenueByPeriod("month", ZONE,
                Instant.parse("2026-01-14T17:00:00Z"),
                Instant.parse("2026-03-10T17:00:00Z"),
                ReportService.REVENUE_STATUSES))
                .thenReturn(List.of(revenueRow("2026-02-01", "100000.00", 1)));

        RevenueReportResponse report = reportService.getRevenue(
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 3, 10), ReportGroupBy.MONTH);

        assertThat(report.points()).extracting(RevenuePoint::period).containsExactly("2026-01", "2026-02", "2026-03");
        assertThat(report.points()).extracting(RevenuePoint::orderCount).containsExactly(0L, 1L, 0L);
    }

    @Test
    void fromAfterTo_isRejected_withoutQueryingDatabase() {
        assertThatThrownBy(() -> reportService.getRevenue(
                LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 4), ReportGroupBy.DAY))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_PARAMETER);
        verifyNoInteractions(reportRepository);
    }

    @Test
    void dailyRange_of366Days_isAllowed() {
        when(reportRepository.sumRevenueByPeriod(anyString(), anyString(), any(Instant.class), any(Instant.class),
                anyCollection())).thenReturn(List.of());

        // 2024 là năm nhuận: 01/01 → 31/12 đúng 366 ngày
        RevenueReportResponse report = reportService.getRevenue(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), ReportGroupBy.DAY);

        assertThat(report.points()).hasSize(366);
        assertThat(report.totalOrders()).isZero();
    }

    @Test
    void dailyRange_over366Days_isRejected() {
        assertThatThrownBy(() -> reportService.getRevenue(
                LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1), ReportGroupBy.DAY))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("groupBy=MONTH");
    }

    @Test
    void monthlyRange_over36Months_isRejected() {
        assertThatThrownBy(() -> reportService.getRevenue(
                LocalDate.of(2020, 1, 1), LocalDate.of(2023, 1, 1), ReportGroupBy.MONTH))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    // ===================== Sách bán chạy =====================

    @Test
    void bestSellers_rankFollowsDatabaseOrder() {
        when(reportRepository.findBestSellers(
                Instant.parse("2026-10-31T17:00:00Z"), Instant.parse("2026-11-30T17:00:00Z"),
                ReportService.REVENUE_STATUSES, 2))
                .thenReturn(List.of(
                        bestSellerRow(7L, "Dế Mèn phiêu lưu ký", 12, "900000.00"),
                        bestSellerRow(3L, "Số đỏ", 5, "600000.00")));

        List<BestSellerResponse> result = reportService.getBestSellers(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), 2);

        assertThat(result).extracting(BestSellerResponse::rank).containsExactly(1, 2);
        assertThat(result).extracting(BestSellerResponse::bookId).containsExactly(7L, 3L);
        assertThat(result.get(0).quantitySold()).isEqualTo(12);
        assertThat(result.get(0).revenue()).isEqualByComparingTo("900000");
    }

    @Test
    void bestSellers_rangeOver36Months_isRejected() {
        assertThatThrownBy(() -> reportService.getBestSellers(
                LocalDate.of(2020, 1, 1), LocalDate.of(2023, 6, 1), 10))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_PARAMETER);
        verifyNoInteractions(reportRepository);
    }

    // ===================== Helper =====================

    private static ReportRepository.RevenueRow revenueRow(String period, String revenue, long orderCount) {
        return new ReportRepository.RevenueRow() {
            @Override
            public String getPeriod() {
                return period;
            }

            @Override
            public BigDecimal getRevenue() {
                return new BigDecimal(revenue);
            }

            @Override
            public Long getOrderCount() {
                return orderCount;
            }
        };
    }

    private static ReportRepository.BestSellerRow bestSellerRow(Long bookId, String title, long quantity, String revenue) {
        return new ReportRepository.BestSellerRow() {
            @Override
            public Long getBookId() {
                return bookId;
            }

            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public Long getQuantitySold() {
                return quantity;
            }

            @Override
            public BigDecimal getRevenue() {
                return new BigDecimal(revenue);
            }
        };
    }
}
