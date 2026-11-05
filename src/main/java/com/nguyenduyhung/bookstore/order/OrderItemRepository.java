package com.nguyenduyhung.bookstore.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Người dùng đã từng MUA VÀ NHẬN cuốn sách này chưa (có đơn COMPLETED chứa nó)?
     *
     * Dấu "_" trong tên method chỉ rõ chỗ đi qua quan hệ: Order_UserId = order.userId,
     * Order_Status = order.status. Không có dấu "_", Spring Data vẫn tự đoán được,
     * nhưng viết rõ ra thì người đọc không phải đoán.
     *
     * "exists" sinh câu SQL dừng ngay khi tìm thấy dòng đầu tiên (LIMIT 1), nhẹ hơn
     * "count" vốn phải đếm hết mọi dòng khớp.
     */
    boolean existsByBookIdAndOrder_UserIdAndOrder_Status(Long bookId, Long userId, OrderStatus status);
}
