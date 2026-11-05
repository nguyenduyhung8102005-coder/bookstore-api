package com.nguyenduyhung.bookstore.order;

import com.nguyenduyhung.bookstore.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderIT extends AbstractIntegrationTest {

    private static final Map<String, String> SHIPPING = Map.of(
            "recipientName", "Nguyễn Văn A",
            "phone", "0912345678",
            "shippingAddress", "1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội");

    // ===================== Luồng chính =====================

    @Test
    void placeOrder_createsPendingOrder_decreasesStock_andClearsCart() throws Exception {
        Long bookId = createSampleBook("75000.00", 5);
        String user = newUserAccessToken();
        addToCart(user, bookId, 2);

        Long orderId = dataId(placeOrder(user)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalAmount").value(150000.0))
                .andExpect(jsonPath("$.data.items[0].bookId").value(bookId))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(75000.0))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2)));

        mockMvc.perform(get("/api/v1/books/{id}", bookId))
                .andExpect(jsonPath("$.data.stockQuantity").value(3));

        mockMvc.perform(get("/api/v1/cart").header(HttpHeaders.AUTHORIZATION, authHeader(user)))
                .andExpect(jsonPath("$.data.items").isEmpty());

        mockMvc.perform(get("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, authHeader(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(orderId))
                .andExpect(jsonPath("$.data.content[0].totalItems").value(2));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId).header(HttpHeaders.AUTHORIZATION, authHeader(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipientName").value("Nguyễn Văn A"));
    }

    @Test
    void emptyCart_cannotBeOrdered() throws Exception {
        placeOrder(newUserAccessToken())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CART_EMPTY"));
    }

    // ===================== Tính nguyên tử (all or nothing) =====================

    /**
     * Giỏ của Alice có 2 sách X và Y. Trong lúc đó Bob mua hết sách Y.
     * Khi Alice đặt hàng, sách X có thể đã được trừ kho TRƯỚC khi phát hiện Y hết
     * hàng; transaction rollback phải trả lại kho cho X, và giỏ Alice giữ nguyên.
     */
    @Test
    void oneBookOutOfStock_rollsBackWholeOrder() throws Exception {
        Long bookX = createSampleBook("50000.00", 5);
        Long bookY = createSampleBook("80000.00", 1);
        String alice = newUserAccessToken();
        String bob = newUserAccessToken();
        addToCart(alice, bookX, 1);
        addToCart(alice, bookY, 1);
        addToCart(bob, bookY, 1);

        placeOrder(bob).andExpect(status().isCreated());

        placeOrder(alice)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        mockMvc.perform(get("/api/v1/books/{id}", bookX))
                .andExpect(jsonPath("$.data.stockQuantity").value(5));
        mockMvc.perform(get("/api/v1/books/{id}", bookY))
                .andExpect(jsonPath("$.data.stockQuantity").value(0));
        mockMvc.perform(get("/api/v1/cart").header(HttpHeaders.AUTHORIZATION, authHeader(alice)))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.hasUnavailableItems").value(true));
    }

    @Test
    void deactivatedBookInCart_cannotBeOrdered() throws Exception {
        Long bookId = createSampleBook("75000.00", 5);
        String user = newUserAccessToken();
        addToCart(user, bookId, 1);
        mockMvc.perform(delete("/api/v1/admin/books/{id}", bookId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader(adminAccessToken())))
                .andExpect(status().isOk());

        placeOrder(user)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_AVAILABLE"));
    }

    // ===================== Bảo mật & validation =====================

    @Test
    void otherUsersOrder_looksLikeItDoesNotExist() throws Exception {
        Long bookId = createSampleBook("75000.00", 5);
        String alice = newUserAccessToken();
        addToCart(alice, bookId, 1);
        Long aliceOrderId = dataId(placeOrder(alice).andExpect(status().isCreated()));

        mockMvc.perform(get("/api/v1/orders/{id}", aliceOrderId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader(newUserAccessToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void invalidShippingInfo_returnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(newUserAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("recipientName", "", "phone", "12345", "shippingAddress", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItems("recipientName", "phone", "shippingAddress")));
    }

    // ===================== Helper =====================

    private void addToCart(String accessToken, Long bookId, int quantity) throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("bookId", bookId, "quantity", quantity))))
                .andExpect(status().isOk());
    }

    private ResultActions placeOrder(String accessToken) throws Exception {
        return mockMvc.perform(post("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, authHeader(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(SHIPPING)));
    }
}
