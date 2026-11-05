package com.nguyenduyhung.bookstore.cart;

import com.nguyenduyhung.bookstore.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartIT extends AbstractIntegrationTest {

    // ===================== Luồng chính =====================

    @Test
    void addingSameBookTwice_mergesIntoOneLine_andComputesTotals() throws Exception {
        Long bookId = createSampleBook("75000.00", 5);
        String user = newUserAccessToken();

        addItem(user, bookId, 2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.totalAmount").value(150000.0));

        addItem(user, bookId, 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].quantity").value(3))
                .andExpect(jsonPath("$.data.items[0].available").value(true))
                .andExpect(jsonPath("$.data.totalAmount").value(225000.0));
    }

    @Test
    void updateQuantity_thenRemoveItem() throws Exception {
        Long bookId = createSampleBook("50000.00", 10);
        String user = newUserAccessToken();
        addItem(user, bookId, 1).andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/cart/items/{bookId}", bookId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("quantity", 4))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].quantity").value(4))
                .andExpect(jsonPath("$.data.totalAmount").value(200000.0));

        mockMvc.perform(delete("/api/v1/cart/items/{bookId}", bookId).header(HttpHeaders.AUTHORIZATION, authHeader(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalAmount").value(0.0));
    }

    @Test
    void clearCart_removesEverything() throws Exception {
        String user = newUserAccessToken();
        addItem(user, createSampleBook("10000.00", 5), 1).andExpect(status().isOk());
        addItem(user, createSampleBook("20000.00", 5), 2).andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/cart").header(HttpHeaders.AUTHORIZATION, authHeader(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalItems").value(0));
    }

    // ===================== Luật nghiệp vụ =====================

    @Test
    void quantityAboveStock_isRejected_evenWhenSplitAcrossTwoAdds() throws Exception {
        Long bookId = createSampleBook("75000.00", 5);
        String user = newUserAccessToken();

        addItem(user, bookId, 6)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        addItem(user, bookId, 3).andExpect(status().isOk());
        addItem(user, bookId, 3)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    /**
     * Sách bị admin ngừng bán SAU khi đã nằm trong giỏ: vẫn giữ trong giỏ nhưng
     * đánh dấu không mua được, để người dùng tự thấy và xử lý. Thêm mới thì bị chặn.
     */
    @Test
    void deactivatedBook_staysInCartAsUnavailable_andCannotBeAddedAgain() throws Exception {
        Long bookId = createSampleBook("75000.00", 5);
        String user = newUserAccessToken();
        addItem(user, bookId, 1).andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/admin/books/{id}", bookId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader(adminAccessToken())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/cart").header(HttpHeaders.AUTHORIZATION, authHeader(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].available").value(false))
                .andExpect(jsonPath("$.data.hasUnavailableItems").value(true));

        addItem(user, bookId, 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_AVAILABLE"));
    }

    // ===================== Bảo mật: mỗi người chỉ thấy giỏ của mình =====================

    @Test
    void users_cannotSeeOrChangeEachOthersCart() throws Exception {
        Long bookId = createSampleBook("75000.00", 5);
        String alice = newUserAccessToken();
        String bob = newUserAccessToken();
        addItem(alice, bookId, 2).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/cart").header(HttpHeaders.AUTHORIZATION, authHeader(bob)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());

        // Bob biết bookId mà Alice đã thêm, nhưng thao tác đó chỉ tìm trong giỏ CỦA BOB
        mockMvc.perform(delete("/api/v1/cart/items/{bookId}", bookId).header(HttpHeaders.AUTHORIZATION, authHeader(bob)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CART_ITEM_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/cart").header(HttpHeaders.AUTHORIZATION, authHeader(alice)))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));
    }

    @Test
    void anonymous_cannotAccessCart() throws Exception {
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isUnauthorized());
    }

    // ===================== Request không hợp lệ =====================

    @Test
    void zeroQuantity_isRejectedByValidation() throws Exception {
        Long bookId = createSampleBook("75000.00", 5);

        addItem(newUserAccessToken(), bookId, 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("quantity"));
    }

    @Test
    void unknownBook_returnsNotFound() throws Exception {
        addItem(newUserAccessToken(), 999_999_999L, 1)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    // ===================== Helper =====================

    private ResultActions addItem(String accessToken, Long bookId, int quantity) throws Exception {
        return mockMvc.perform(post("/api/v1/cart/items")
                .header(HttpHeaders.AUTHORIZATION, authHeader(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("bookId", bookId, "quantity", quantity))));
    }
}
