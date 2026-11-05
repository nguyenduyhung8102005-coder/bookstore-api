package com.nguyenduyhung.bookstore.report;

import com.nguyenduyhung.bookstore.common.config.OpenApiConfig;
import com.nguyenduyhung.bookstore.common.response.ApiResponse;
import com.nguyenduyhung.bookstore.report.dto.BestSellerResponse;
import com.nguyenduyhung.bookstore.report.dto.RevenueReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Báo cáo cho admin. from/to là NGÀY theo giờ Việt Nam (yyyy-MM-dd), tính cả hai đầu.
 * Doanh thu tính theo ngày TRẢ TIỀN (paid_at), chỉ gồm đơn PAID/SHIPPING/COMPLETED.
 */
@Tag(name = "Admin - Reports")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @Operation(summary = "Doanh thu theo ngày hoặc theo tháng (có đủ mọi kỳ, kỳ không có đơn = 0)")
    @GetMapping("/revenue")
    public ApiResponse<RevenueReportResponse> revenue(
            @Parameter(example = "2026-11-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(example = "2026-11-30") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") ReportGroupBy groupBy) {
        return ApiResponse.success(reportService.getRevenue(from, to, groupBy));
    }

    @Operation(summary = "Sách bán chạy nhất theo số lượng đã bán")
    @GetMapping("/best-sellers")
    public ApiResponse<List<BestSellerResponse>> bestSellers(
            @Parameter(example = "2026-11-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(example = "2026-11-30") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return ApiResponse.success(reportService.getBestSellers(from, to, limit));
    }
}
