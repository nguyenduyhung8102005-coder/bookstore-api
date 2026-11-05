package com.nguyenduyhung.bookstore.review;

import com.nguyenduyhung.bookstore.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewIT extends AbstractIntegrationTest {

    // ===================== Ai được viết đánh giá =====================

    @Test
    void userWhoNeverBoughtTheBook_cannotReview() throws Exception {
        Long bookId = createSampleBook("75000.00", 5);

        writeReview(newUserAccessToken(), bookId, 5)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REVIEW_NOT_ALLOWED"));
    }

    /** Đã trả tiền nhưng chưa nhận hàng (đơn chưa COMPLETED) thì chưa được đánh giá. */
    @Test
    void paidButNotYetCompletedOrder_isNotEnough() throws Exception {
        Long bookId = createSampleBook("75000.00", 5);
        String user = newUserAccessToken();
        Long orderId = createOrder(user, bookId, 1);
        pay(user, orderId);

        writeReview(user, bookId, 5)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REVIEW_NOT_ALLOWED"));
    }

    @Test
    void buyerWithCompletedOrder_canReviewOnce_andRatingShowsOnBookDetail() throws Exception {
        Long bookId = createSampleBook("75000.00", 5);
        String user = buyerWithCompletedOrder(bookId);

        writeReview(user, bookId, 4)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(4))
                .andExpect(jsonPath("$.data.reviewerName").value("IT User"));

        writeReview(user, bookId, 5)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REVIEW_ALREADY_EXISTS"));

        mockMvc.perform(get("/api/v1/books/{id}", bookId))
                .andExpect(jsonPath("$.data.averageRating").value(4.0))
                .andExpect(jsonPath("$.data.reviewCount").value(1));
    }

    @Test
    void averageRating_isComputedAcrossReviewers() throws Exception {
        Long bookId = createSampleBook("75000.00", 10);
        writeReview(buyerWithCompletedOrder(bookId), bookId, 4).andExpect(status().isCreated());
        writeReview(buyerWithCompletedOrder(bookId), bookId, 5).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/books/{id}", bookId))
                .andExpect(jsonPath("$.data.averageRating").value(4.5))
                .andExpect(jsonPath("$.data.reviewCount").value(2));

        // Ai cũng xem được danh sách đánh giá, không cần đăng nhập
        mockMvc.perform(get("/api/v1/books/{id}/reviews", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void bookWithoutReviews_hasNullAverageAndZeroCount() throws Exception {
        Long bookId = createSampleBook("75000.00", 5);

        mockMvc.perform(get("/api/v1/books/{id}", bookId))
                .andExpect(jsonPath("$.data.averageRating").isEmpty())
                .andExpect(jsonPath("$.data.reviewCount").value(0));
    }

    @Test
    void ratingOutOfRange_isRejected() throws Exception {
        Long bookId = createSampleBook("75000.00", 5);

        writeReview(newUserAccessToken(), bookId, 6)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("rating"));
    }

    // ===================== Sửa / xóa =====================

    @Test
    void onlyAuthorCanEdit_andAdminCanDelete() throws Exception {
        Long bookId = createSampleBook("75000.00", 5);
        String author = buyerWithCompletedOrder(bookId);
        Long reviewId = dataId(writeReview(author, bookId, 2).andExpect(status().isCreated()));

        // Người khác sửa: 403 (đánh giá là thông tin công khai nên không cần giấu bằng 404)
        editReview(newUserAccessToken(), reviewId, 1)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        editReview(author, reviewId, 5)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(5));

        mockMvc.perform(delete("/api/v1/reviews/{id}", reviewId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader(adminAccessToken())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/books/{id}", bookId))
                .andExpect(jsonPath("$.data.reviewCount").value(0));
    }

    @Test
    void anonymous_cannotWriteReview() throws Exception {
        Long bookId = createSampleBook("75000.00", 5);

        mockMvc.perform(post("/api/v1/books/{id}/reviews", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("rating", 5))))
                .andExpect(status().isUnauthorized());
    }

    // ===================== Helper =====================

    /** Tạo một người mua mới có đơn COMPLETED chứa cuốn sách: mua → trả tiền → admin giao → hoàn thành. */
    private String buyerWithCompletedOrder(Long bookId) throws Exception {
        String buyer = newUserAccessToken();
        Long orderId = createOrder(buyer, bookId, 1);
        pay(buyer, orderId);
        adminSetStatus(orderId, "SHIPPING");
        adminSetStatus(orderId, "COMPLETED");
        return buyer;
    }

    private void pay(String accessToken, Long orderId) throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/pay", orderId)
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

    private ResultActions writeReview(String accessToken, Long bookId, int rating) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("rating", rating);
        body.put("comment", "Nhận xét " + rating + " sao");
        return mockMvc.perform(post("/api/v1/books/{id}/reviews", bookId)
                .header(HttpHeaders.AUTHORIZATION, authHeader(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body)));
    }

    private ResultActions editReview(String accessToken, Long reviewId, int rating) throws Exception {
        return mockMvc.perform(put("/api/v1/reviews/{id}", reviewId)
                .header(HttpHeaders.AUTHORIZATION, authHeader(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("rating", rating, "comment", "Đã sửa"))));
    }
}
