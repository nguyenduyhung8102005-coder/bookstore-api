package com.nguyenduyhung.bookstore.order;

/**
 * Trạng thái đơn hàng và LUẬT chuyển trạng thái (state machine):
 *
 *   PENDING ──pay──> PAID ──ship──> SHIPPING ──complete──> COMPLETED
 *      │               │
 *      └──cancel──> CANCELLED <──cancel──┘
 *
 * Luật nằm ngay trong enum: chỉ có MỘT nơi định nghĩa "được đi từ đâu tới đâu",
 * mọi chỗ khác (user, admin, job) đều hỏi lại enum. Thêm trạng thái mới thì
 * trình biên dịch buộc phải xử lý nó trong switch bên dưới (switch expression
 * trên enum phải liệt kê đủ mọi giá trị).
 */
public enum OrderStatus {
    PENDING,
    PAID,
    SHIPPING,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING -> target == PAID || target == CANCELLED;
            case PAID -> target == SHIPPING || target == CANCELLED;
            case SHIPPING -> target == COMPLETED;
            // Trạng thái cuối: không đi đâu được nữa.
            case COMPLETED, CANCELLED -> false;
        };
    }
}
