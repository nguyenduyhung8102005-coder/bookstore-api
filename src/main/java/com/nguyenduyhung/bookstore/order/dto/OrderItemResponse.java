package com.nguyenduyhung.bookstore.order.dto;

import java.math.BigDecimal;

/** bookTitle và unitPrice là giá trị lúc mua (snapshot), không phải giá hiện tại của sách. */
public record OrderItemResponse(
        Long bookId,
        String bookTitle,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
}
