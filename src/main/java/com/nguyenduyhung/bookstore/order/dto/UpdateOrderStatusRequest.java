package com.nguyenduyhung.bookstore.order.dto;

import com.nguyenduyhung.bookstore.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Trạng thái MỚI mà admin muốn đặt: SHIPPING, COMPLETED hoặc CANCELLED.
 * Gửi giá trị không có trong enum (ví dụ "DONE") sẽ bị Jackson từ chối ngay
 * khi đọc JSON và trả 400 INVALID_REQUEST_BODY.
 */
public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
}
