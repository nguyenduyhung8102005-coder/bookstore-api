package com.nguyenduyhung.bookstore.cart;

import com.nguyenduyhung.bookstore.cart.dto.CartItemResponse;
import com.nguyenduyhung.bookstore.cart.dto.CartResponse;
import com.nguyenduyhung.bookstore.catalog.book.Book;

import java.math.BigDecimal;
import java.util.List;

/**
 * Chuyển danh sách CartItem thành CartResponse và tính các con số tổng.
 *
 * Về tính tiền bằng BigDecimal:
 * - KHÔNG dùng double: 0.1 + 0.2 = 0.30000000000000004. Sai số nhỏ, nhưng cộng dồn
 *   qua hàng nghìn đơn thì sổ sách lệch.
 * - Nhân giá (2 chữ số thập phân) với số nguyên luôn cho kết quả CHÍNH XÁC, vẫn 2 chữ
 *   số thập phân, không cần làm tròn. Chỉ phép CHIA (ví dụ tính giảm giá theo %) mới
 *   cần chỉ định RoundingMode, nếu không BigDecimal sẽ ném ArithmeticException.
 * - So sánh hai BigDecimal dùng compareTo, không dùng equals: new BigDecimal("1.0")
 *   .equals(new BigDecimal("1.00")) là false vì khác scale.
 */
public final class CartMapper {

    /** 0.00 (scale 2) thay vì BigDecimal.ZERO (scale 0), để giỏ trống vẫn trả "0.00" đồng nhất. */
    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO.setScale(2);

    private CartMapper() {
    }

    public static CartResponse toResponse(List<CartItem> items) {
        List<CartItemResponse> lines = items.stream().map(CartMapper::toItemResponse).toList();

        int totalItems = lines.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal totalAmount = lines.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(ZERO_AMOUNT, BigDecimal::add);
        boolean hasUnavailableItems = lines.stream().anyMatch(line -> !line.available());

        return new CartResponse(lines, totalItems, totalAmount, hasUnavailableItems);
    }

    static CartItemResponse toItemResponse(CartItem item) {
        Book book = item.getBook();
        BigDecimal lineTotal = book.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        boolean available = book.isActive() && book.getStockQuantity() >= item.getQuantity();
        return new CartItemResponse(
                book.getId(), book.getTitle(), book.getPrice(), item.getQuantity(), lineTotal, available);
    }
}
