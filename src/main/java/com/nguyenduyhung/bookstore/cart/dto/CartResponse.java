package com.nguyenduyhung.bookstore.cart.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param totalItems          tổng số cuốn (cộng quantity của mọi dòng)
 * @param totalAmount         tổng tiền của mọi dòng theo giá hiện tại
 * @param hasUnavailableItems true nếu có dòng không thể đặt hàng (client nên cảnh báo)
 */
public record CartResponse(
        List<CartItemResponse> items,
        int totalItems,
        BigDecimal totalAmount,
        boolean hasUnavailableItems
) {
}
