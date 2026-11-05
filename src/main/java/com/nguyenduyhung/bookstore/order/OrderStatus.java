package com.nguyenduyhung.bookstore.order;

/**
 * Trạng thái đơn hàng:
 *   PENDING -> PAID -> SHIPPING -> COMPLETED
 *   PENDING / PAID -> CANCELLED
 * Luật chuyển trạng thái (state machine) sẽ được cài ở GĐ 5b.
 */
public enum OrderStatus {
    PENDING,
    PAID,
    SHIPPING,
    COMPLETED,
    CANCELLED
}
