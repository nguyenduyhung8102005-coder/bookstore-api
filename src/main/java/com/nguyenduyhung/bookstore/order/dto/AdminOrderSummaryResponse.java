package com.nguyenduyhung.bookstore.order.dto;

import com.nguyenduyhung.bookstore.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/** Một dòng trong danh sách đơn của admin: có thêm userId để biết đơn của ai. */
public record AdminOrderSummaryResponse(
        Long id,
        Long userId,
        OrderStatus status,
        BigDecimal totalAmount,
        int totalItems,
        String recipientName,
        Instant createdAt
) {
}
