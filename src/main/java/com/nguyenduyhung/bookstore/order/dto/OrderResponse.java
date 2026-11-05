package com.nguyenduyhung.bookstore.order.dto;

import com.nguyenduyhung.bookstore.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Chi tiết đầy đủ một đơn hàng. */
public record OrderResponse(
        Long id,
        OrderStatus status,
        BigDecimal totalAmount,
        String recipientName,
        String phone,
        String shippingAddress,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant paidAt,
        Instant cancelledAt
) {
}
