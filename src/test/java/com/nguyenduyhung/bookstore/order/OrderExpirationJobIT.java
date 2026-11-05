package com.nguyenduyhung.bookstore.order;

import com.nguyenduyhung.bookstore.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lịch tự chạy của job đã bị tắt trong profile test (app.scheduling.enabled=false).
 * Test gọi thẳng method của job, nên biết chính xác lúc nào job chạy.
 * Thay vì chờ 30 phút thật, test "tua ngược" created_at của đơn trong DB.
 */
class OrderExpirationJobIT extends AbstractIntegrationTest {

    @Autowired
    private OrderExpirationJob orderExpirationJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void expiredPendingOrder_isCancelled_andStockIsRestored() throws Exception {
        Long bookId = createSampleBook("75000.00", 5);
        Long orderId = createOrder(newUserAccessToken(), bookId, 2);
        assertThat(stockOf(bookId)).isEqualTo(3);
        makeOrderOlderBy(orderId, 31);

        orderExpirationJob.cancelExpiredOrders();

        assertThat(statusOf(orderId)).isEqualTo("CANCELLED");
        assertThat(stockOf(bookId)).isEqualTo(5);
    }

    @Test
    void recentPendingOrders_andPaidOrders_areNotTouched() throws Exception {
        Long bookId = createSampleBook("75000.00", 5);

        Long recentOrderId = createOrder(newUserAccessToken(), bookId, 1);

        String payer = newUserAccessToken();
        Long paidOrderId = createOrder(payer, bookId, 1);
        mockMvc.perform(post("/api/v1/orders/{id}/pay", paidOrderId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader(payer)))
                .andExpect(status().isOk());
        makeOrderOlderBy(paidOrderId, 120); // đã thanh toán, dù cũ đến đâu cũng không bị hủy

        orderExpirationJob.cancelExpiredOrders();

        assertThat(statusOf(recentOrderId)).isEqualTo("PENDING");
        assertThat(statusOf(paidOrderId)).isEqualTo("PAID");
        assertThat(stockOf(bookId)).isEqualTo(3);
    }

    // ===================== Helper =====================

    private void makeOrderOlderBy(Long orderId, int minutes) {
        jdbcTemplate.update("UPDATE orders SET created_at = created_at - make_interval(mins => ?) WHERE id = ?",
                minutes, orderId);
    }

    private String statusOf(Long orderId) {
        return jdbcTemplate.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId);
    }

    private int stockOf(Long bookId) {
        return jdbcTemplate.queryForObject("SELECT stock_quantity FROM books WHERE id = ?", Integer.class, bookId);
    }
}
