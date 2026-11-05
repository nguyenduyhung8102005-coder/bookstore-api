package com.nguyenduyhung.bookstore.order.dto;

import com.nguyenduyhung.bookstore.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/** Một dòng trong danh sách "Đơn hàng của tôi". */
public record OrderSummaryResponse(
        Long id,
        OrderStatus status,
        BigDecimal totalAmount,
        int totalItems,
        Instant createdAt
) {
}
