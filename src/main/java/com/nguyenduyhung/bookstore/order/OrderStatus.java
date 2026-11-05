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

    /**
     * Đơn ở trạng thái này có được tính vào doanh thu không (dùng cho thống kê).
     *
     * Chỉ tính đơn ĐÃ TRẢ TIỀN và CHƯA HỦY. Đơn PAID rồi bị hủy cũng có paid_at
     * nhưng không tính, vì giả định tiền đã được hoàn lại cho khách.
     */
    public boolean countsAsRevenue() {
        return switch (this) {
            case PAID, SHIPPING, COMPLETED -> true;
            case PENDING, CANCELLED -> false;
        };
    }
}
