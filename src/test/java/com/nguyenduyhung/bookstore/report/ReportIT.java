package com.nguyenduyhung.bookstore.report;

import com.nguyenduyhung.bookstore.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Thống kê với dữ liệu biết trước kết quả.
 *
 * Các test khác cũng tạo đơn đã trả tiền (paid_at = lúc chạy test), nên ở đây mỗi
 * test "dời" paid_at của đơn mình tạo về một khoảng thời gian riêng ở quá khứ
 * (năm 2001, 2002...) bằng JdbcTemplate, rồi chỉ hỏi báo cáo đúng khoảng đó.
 * Nhờ vậy kết quả không phụ thuộc test nào chạy trước.
 */
class ReportIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ===================== Doanh thu =====================

    /** Ranh giới ngày tính theo giờ Việt Nam (UTC+7), không phải theo UTC. */
    @Test
    void dailyRevenue_splitsDaysByVietnamTime_andFillsEmptyDays() throws Exception {
        Long bookId = createSampleBook("100000.00", 10);
        String buyer = newUserAccessToken();

        // 16:59:59Z ngày 09/03 = 23:59:59 ngày 09/03 giờ VN → ngoài khoảng báo cáo
        paidOrder(buyer, bookId, 1, "2001-03-09T16:59:59Z");
        // 16:59Z ngày 10/03 = 23:59 ngày 10/03 giờ VN → ngày 10/03
        paidOrder(buyer, bookId, 1, "2001-03-10T16:59:00Z");
        // 17:00Z ngày 10/03 = 00:00 ngày 11/03 giờ VN → ngày 11/03 (theo UTC thì vẫn là 10/03)
        paidOrder(buyer, bookId, 2, "2001-03-10T17:00:00Z");

        revenue("2001-03-10", "2001-03-12", "DAY")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timeZone").value("Asia/Ho_Chi_Minh"))
                .andExpect(jsonPath("$.data.points", hasSize(3)))
                .andExpect(jsonPath("$.data.points[0].period").value("2001-03-10"))
                .andExpect(jsonPath("$.data.points[0].revenue").value(100000.0))
                .andExpect(jsonPath("$.data.points[0].orderCount").value(1))
                .andExpect(jsonPath("$.data.points[1].period").value("2001-03-11"))
                .andExpect(jsonPath("$.data.points[1].revenue").value(200000.0))
                .andExpect(jsonPath("$.data.points[2].period").value("2001-03-12"))
                .andExpect(jsonPath("$.data.points[2].revenue").value(0.0))
                .andExpect(jsonPath("$.data.points[2].orderCount").value(0))
                .andExpect(jsonPath("$.data.totalRevenue").value(300000.0))
                .andExpect(jsonPath("$.data.totalOrders").value(2));
    }

    /** Chỉ tính đơn đã trả tiền và chưa hủy: PAID, SHIPPING, COMPLETED. */
    @Test
    void revenue_countsOnlyPaidShippingAndCompletedOrders() throws Exception {
        Long bookId = createSampleBook("50000.00", 10);
        String buyer = newUserAccessToken();
        String paidAt = "2002-05-01T03:00:00Z";

        paidOrder(buyer, bookId, 1, paidAt);                                         // PAID
        Long shipping = paidOrder(buyer, bookId, 1, paidAt);
        adminSetStatus(shipping, "SHIPPING");                                         // SHIPPING
        Long completed = paidOrder(buyer, bookId, 1, paidAt);
        adminSetStatus(completed, "SHIPPING");
        adminSetStatus(completed, "COMPLETED");                                       // COMPLETED
        Long refunded = paidOrder(buyer, bookId, 1, paidAt);
        userAction(buyer, refunded, "cancel");                                        // trả tiền rồi hủy → không tính
        createOrder(buyer, bookId, 1);                                                // PENDING, chưa trả tiền → không tính

        revenue("2002-05-01", "2002-05-01", "DAY")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRevenue").value(150000.0))
                .andExpect(jsonPath("$.data.totalOrders").value(3));
    }

    @Test
    void monthlyRevenue_groupsByVietnamMonth() throws Exception {
        Long bookId = createSampleBook("40000.00", 10);
        String buyer = newUserAccessToken();

        paidOrder(buyer, bookId, 1, "2003-01-20T03:00:00Z");
        // 18:00Z ngày 31/01 = 01:00 ngày 01/02 giờ VN → tính vào tháng 2
        paidOrder(buyer, bookId, 1, "2003-01-31T18:00:00Z");
        paidOrder(buyer, bookId, 1, "2003-02-10T03:00:00Z");

        revenue("2003-01-15", "2003-03-10", "MONTH")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.points", hasSize(3)))
                .andExpect(jsonPath("$.data.points[0].period").value("2003-01"))
                .andExpect(jsonPath("$.data.points[0].orderCount").value(1))
                .andExpect(jsonPath("$.data.points[1].period").value("2003-02"))
                .andExpect(jsonPath("$.data.points[1].orderCount").value(2))
                .andExpect(jsonPath("$.data.points[1].revenue").value(80000.0))
                .andExpect(jsonPath("$.data.points[2].period").value("2003-03"))
                .andExpect(jsonPath("$.data.points[2].orderCount").value(0));
    }

    // ===================== Sách bán chạy =====================

    @Test
    void bestSellers_rankedByQuantity_excludingCancelledAndUnpaidOrders() throws Exception {
        Long bookA = createSampleBook("30000.00", 20);
        Long bookB = createSampleBook("80000.00", 20);
        Long bookC = createSampleBook("20000.00", 20);
        String buyer = newUserAccessToken();
        String paidAt = "2004-06-15T03:00:00Z";

        paidOrder(buyer, bookA, 2, paidAt);
        paidOrder(buyer, bookA, 1, paidAt);
        paidOrder(buyer, bookB, 1, paidAt);
        // Sách C "mua" nhiều nhất nhưng đơn bị hủy hoặc chưa trả tiền → không được xếp hạng
        Long cancelled = paidOrder(buyer, bookC, 5, paidAt);
        userAction(buyer, cancelled, "cancel");
        createOrder(buyer, bookC, 4);

        bestSellers("2004-06-01", "2004-06-30", 10)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].rank").value(1))
                .andExpect(jsonPath("$.data[0].bookId").value(bookA.intValue()))
                .andExpect(jsonPath("$.data[0].quantitySold").value(3))
                .andExpect(jsonPath("$.data[0].revenue").value(90000.0))
                .andExpect(jsonPath("$.data[1].bookId").value(bookB.intValue()))
                .andExpect(jsonPath("$.data[1].quantitySold").value(1));

        bestSellers("2004-06-01", "2004-06-30", 1)
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].bookId").value(bookA.intValue()));
    }

    // ===================== Tham số sai, phân quyền =====================

    @Test
    void invalidParameters_areRejectedWith400() throws Exception {
        revenue("2026-11-05", "2026-11-01", "DAY")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));

        revenue("2024-01-01", "2025-06-01", "DAY")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));

        revenue("2026-13-01", "2026-12-01", "DAY")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));

        revenue("2026-11-01", "2026-11-30", "WEEK")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));

        mockMvc.perform(get("/api/v1/admin/reports/revenue").param("to", "2026-11-30")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(adminAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));

        bestSellers("2026-11-01", "2026-11-30", 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("limit"));
    }

    @Test
    void groupBy_defaultsToDay() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/revenue")
                        .param("from", "2026-11-01").param("to", "2026-11-07")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(adminAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupBy").value("DAY"))
                .andExpect(jsonPath("$.data.points", hasSize(7)));
    }

    @Test
    void onlyAdminCanSeeReports() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/revenue")
                        .param("from", "2026-11-01").param("to", "2026-11-30")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(newUserAccessToken())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/reports/best-sellers")
                        .param("from", "2026-11-01").param("to", "2026-11-30"))
                .andExpect(status().isUnauthorized());
    }

    // ===================== Helper =====================

    /** Tạo đơn, trả tiền qua API, rồi dời paid_at về thời điểm cho trước (chuỗi ISO, UTC). */
    private Long paidOrder(String buyer, Long bookId, int quantity, String paidAtUtc) throws Exception {
        Long orderId = createOrder(buyer, bookId, quantity);
        userAction(buyer, orderId, "pay");
        jdbcTemplate.update("UPDATE orders SET paid_at = CAST(? AS TIMESTAMPTZ) WHERE id = ?", paidAtUtc, orderId);
        return orderId;
    }

    private void userAction(String accessToken, Long orderId, String action) throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/{action}", orderId, action)
                        .header(HttpHeaders.AUTHORIZATION, authHeader(accessToken)))
                .andExpect(status().isOk());
    }

    private void adminSetStatus(Long orderId, String status) throws Exception {
        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("status", status))))
                .andExpect(status().isOk());
    }

    private ResultActions revenue(String from, String to, String groupBy) throws Exception {
        return mockMvc.perform(get("/api/v1/admin/reports/revenue")
                .param("from", from).param("to", to).param("groupBy", groupBy)
                .header(HttpHeaders.AUTHORIZATION, authHeader(adminAccessToken())));
    }

    private ResultActions bestSellers(String from, String to, int limit) throws Exception {
        return mockMvc.perform(get("/api/v1/admin/reports/best-sellers")
                .param("from", from).param("to", to).param("limit", String.valueOf(limit))
                .header(HttpHeaders.AUTHORIZATION, authHeader(adminAccessToken())));
    }
}
