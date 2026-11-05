package com.nguyenduyhung.bookstore.cart.dto;

import com.nguyenduyhung.bookstore.cart.CartItem;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Lưu ý: KHÔNG có userId trong request. Giỏ của ai luôn được xác định từ access
 * token; nếu nhận userId từ client, ai cũng thêm được hàng vào giỏ người khác.
 */
public record AddCartItemRequest(
        @NotNull @Positive
        Long bookId,

        // Hằng số dùng được trong annotation vì là "static final int" (giá trị biết lúc compile).
        @NotNull @Min(1) @Max(CartItem.MAX_QUANTITY)
        Integer quantity
) {
}
