package com.nguyenduyhung.bookstore.order;

import com.nguyenduyhung.bookstore.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Vòng đời đơn hàng qua HTTP thật: thanh toán, hủy, admin chuyển trạng thái. */
class OrderLifecycleIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ===================== Người mua =====================

    @Test
    void pay_movesPendingToPaid_andCannotPayTwice() throws Exception {
        String user = newUserAccessToken();
        Long orderId = createOrder(user, createSampleBook("75000.00", 5), 1);

        userAction(user, orderId, "pay")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.paidAt").exists());

        userAction(user, orderId, "pay")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS_TRANSITION"));
    }

    @Test
    void cancelPendingOrder_restoresStock() throws Exception {
        Long bookId = createSampleBook("75000.00", 5);
        String user = newUserAccessToken();
        Long orderId = createOrder(user, bookId, 2);
        expectStock(bookId, 3);

        userAction(user, orderId, "cancel")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelledAt").exists());

        expectStock(bookId, 5);
    }

    /** Job chạy mỗi phút; trong khoảng chờ đó, đơn quá hạn vẫn không thanh toán được. */
    @Test
    void payingExpiredOrder_isRejected() throws Exception {
        String user = newUserAccessToken();
        Long orderId = createOrder(user, createSampleBook("75000.00", 5), 1);
        makeOrderOlderBy(orderId, 31);

        userAction(user, orderId, "pay")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_EXPIRED"));
    }

    @Test
    void otherUser_cannotPayOrCancelMyOrder() throws Exception {
        Long orderId = createOrder(newUserAccessToken(), createSampleBook("75000.00", 5), 1);
        String stranger = newUserAccessToken();

        userAction(stranger, orderId, "pay").andExpect(status().isNotFound());
        userAction(stranger, orderId, "cancel").andExpect(status().isNotFound());
    }

    // ===================== Admin =====================

    @Test
    void fullLifecycle_paidThenShippedThenCompleted() throws Exception {
        String user = newUserAccessToken();
        Long orderId = createOrder(user, createSampleBook("75000.00", 5), 1);
        userAction(user, orderId, "pay").andExpect(status().isOk());

        adminChangeStatus(orderId, "SHIPPING")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPING"));

        // Đơn đang giao: người mua không hủy được nữa
        userAction(user, orderId, "cancel")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS_TRANSITION"));

        adminChangeStatus(orderId, "COMPLETED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // COMPLETED là trạng thái cuối
        adminChangeStatus(orderId, "CANCELLED").andExpect(status().isConflict());
    }

    @Test
    void admin_cannotShipUnpaidOrder() throws Exception {
        Long orderId = createOrder(newUserAccessToken(), createSampleBook("75000.00", 5), 1);

        adminChangeStatus(orderId, "SHIPPING")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS_TRANSITION"));
    }

    @Test
    void admin_cannotMarkOrderAsPaid() throws Exception {
        Long orderId = createOrder(newUserAccessToken(), createSampleBook("75000.00", 5), 1);

        adminChangeStatus(orderId, "PAID")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void adminCancelsPaidOrder_stockIsRestored() throws Exception {
        Long bookId = createSampleBook("75000.00", 5);
        String user = newUserAccessToken();
        Long orderId = createOrder(user, bookId, 2);
        userAction(user, orderId, "pay").andExpect(status().isOk());

        adminChangeStatus(orderId, "CANCELLED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        expectStock(bookId, 5);
    }

    @Test
    void adminOrderList_canBeFilteredByStatus() throws Exception {
        String user = newUserAccessToken();
        Long orderId = createOrder(user, createSampleBook("75000.00", 5), 1);
        userAction(user, orderId, "pay").andExpect(status().isOk());
        String admin = adminAccessToken();

        mockMvc.perform(get("/api/v1/admin/orders").param("status", "PAID").param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].id").value(hasItem(orderId.intValue())));

        mockMvc.perform(get("/api/v1/admin/orders").param("status", "CANCELLED").param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].id").value(not(hasItem(orderId.intValue()))));
    }

    @Test
    void regularUser_cannotUseAdminOrderEndpoints() throws Exception {
        String user = newUserAccessToken();
        Long orderId = createOrder(user, createSampleBook("75000.00", 5), 1);

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("status", "SHIPPING"))))
                .andExpect(status().isForbidden());
    }

    // ===================== Helper =====================

    private ResultActions userAction(String accessToken, Long orderId, String action) throws Exception {
        return mockMvc.perform(post("/api/v1/orders/{id}/" + action, orderId)
                .header(HttpHeaders.AUTHORIZATION, authHeader(accessToken)));
    }

    private ResultActions adminChangeStatus(Long orderId, String status) throws Exception {
        return mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
                .header(HttpHeaders.AUTHORIZATION, authHeader(adminAccessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("status", status))));
    }

    private void expectStock(Long bookId, int expected) throws Exception {
        mockMvc.perform(get("/api/v1/books/{id}", bookId))
                .andExpect(jsonPath("$.data.stockQuantity").value(expected));
    }

    /** "Tua ngược" thời điểm tạo đơn thay vì phải chờ thật 30 phút. */
    private void makeOrderOlderBy(Long orderId, int minutes) {
        jdbcTemplate.update("UPDATE orders SET created_at = created_at - make_interval(mins => ?) WHERE id = ?",
                minutes, orderId);
    }
}
