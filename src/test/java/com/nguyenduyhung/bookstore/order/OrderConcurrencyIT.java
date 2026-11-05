package com.nguyenduyhung.bookstore.order;

import com.nguyenduyhung.bookstore.AbstractIntegrationTest;
import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.order.dto.PlaceOrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test quan trọng nhất của dự án: nhiều người mua cùng một cuốn sách CÙNG LÚC.
 *
 * Gọi thẳng OrderService từ nhiều luồng (thread), mỗi luồng là một người mua.
 * Một CountDownLatch giữ mọi luồng ở vạch xuất phát cho đến khi tất cả sẵn sàng,
 * rồi thả cùng lúc, để các transaction thật sự chồng lên nhau trên PostgreSQL thật.
 *
 * Lưu ý: thứ tự chạy của các luồng là không đoán trước được, nên test chỉ khẳng
 * định những điều LUÔN đúng dù luồng nào chạy trước (bất biến - invariant).
 */
class OrderConcurrencyIT extends AbstractIntegrationTest {

    private static final PlaceOrderRequest SHIPPING =
            new PlaceOrderRequest("Nguyễn Văn A", "0912345678", "1 Đại Cồ Việt, Hà Nội");

    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void twoBuyersForTheLastCopy_exactlyOneSucceeds() throws Exception {
        Long bookId = createSampleBook("75000.00", 1);
        List<Long> buyers = buyersWithBookInCart(bookId, 2);

        List<String> outcomes = placeOrdersAtTheSameTime(buyers);

        // Người thua: hoặc thấy hết hàng ngay, hoặc va chạm optimistic lock rồi thử
        // lại và lúc đó mới thấy hết hàng. Cả hai đường đều kết thúc ở INSUFFICIENT_STOCK.
        assertThat(outcomes).containsExactlyInAnyOrder("OK", "INSUFFICIENT_STOCK");
        assertThat(stockOf(bookId)).isZero();
        assertThat(quantitySoldOf(bookId)).isEqualTo(1);
    }

    /**
     * 8 người tranh 3 cuốn. Dù các luồng xen kẽ thế nào, hệ thống không bao giờ
     * được bán quá số lượng (oversell) hay "làm mất" một lần trừ kho (lost update).
     */
    @Test
    void manyBuyersForFewCopies_neverOversells() throws Exception {
        int initialStock = 3;
        Long bookId = createSampleBook("75000.00", initialStock);
        List<Long> buyers = buyersWithBookInCart(bookId, 8);

        List<String> outcomes = placeOrdersAtTheSameTime(buyers);

        long successes = outcomes.stream().filter("OK"::equals).count();
        int remainingStock = stockOf(bookId);
        long sold = quantitySoldOf(bookId);

        assertThat(remainingStock).isGreaterThanOrEqualTo(0);
        assertThat(successes).isLessThanOrEqualTo(initialStock);
        // Bất biến quan trọng nhất: số đã bán + số còn lại = số ban đầu.
        assertThat(sold + remainingStock).isEqualTo(initialStock);
        assertThat(sold).isEqualTo(successes);
        assertThat(outcomes).allMatch(o -> o.equals("OK") || o.equals("INSUFFICIENT_STOCK")
                || o.equals("CONCURRENT_UPDATE"));
    }

    // ===================== Helper =====================

    /** Tạo N người mua, mỗi người có 1 cuốn sách trong giỏ. Trả về danh sách userId. */
    private List<Long> buyersWithBookInCart(Long bookId, int count) throws Exception {
        List<Long> userIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String token = newUserAccessToken();
            mockMvc.perform(post("/api/v1/cart/items")
                            .header(HttpHeaders.AUTHORIZATION, authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(Map.of("bookId", bookId, "quantity", 1))))
                    .andExpect(status().isOk());
            userIds.add(dataId(mockMvc.perform(get("/api/v1/users/me")
                    .header(HttpHeaders.AUTHORIZATION, authHeader(token)))));
        }
        return userIds;
    }

    /** Mọi người mua cùng bấm "Đặt hàng" một lúc. Kết quả: "OK" hoặc mã lỗi. */
    private List<String> placeOrdersAtTheSameTime(List<Long> userIds) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(userIds.size());
        CountDownLatch allReady = new CountDownLatch(userIds.size());
        CountDownLatch startSignal = new CountDownLatch(1);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (Long userId : userIds) {
                futures.add(pool.submit(() -> {
                    allReady.countDown();
                    startSignal.await(); // đứng chờ ở vạch xuất phát
                    try {
                        orderService.placeOrder(userId, SHIPPING);
                        return "OK";
                    } catch (AppException e) {
                        return e.getErrorCode().name();
                    }
                }));
            }
            assertThat(allReady.await(10, TimeUnit.SECONDS)).isTrue();
            startSignal.countDown(); // xuất phát!

            List<String> outcomes = new ArrayList<>();
            for (Future<String> future : futures) {
                outcomes.add(future.get(60, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    /** Đọc thẳng từ DB bằng JdbcTemplate, không qua Hibernate, để chắc chắn là dữ liệu đã commit. */
    private int stockOf(Long bookId) {
        return jdbcTemplate.queryForObject("SELECT stock_quantity FROM books WHERE id = ?", Integer.class, bookId);
    }

    private long quantitySoldOf(Long bookId) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(quantity), 0) FROM order_items WHERE book_id = ?", Long.class, bookId);
    }
}
