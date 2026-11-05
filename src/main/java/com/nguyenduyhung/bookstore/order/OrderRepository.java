package com.nguyenduyhung.bookstore.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Tìm đơn theo id VÀ chủ sở hữu. Đơn của người khác trả về rỗng (→ 404), y
     * như đơn không tồn tại: không trả 403, để người lạ không dò được id nào có thật.
     * Lấy luôn items trong cùng câu SQL vì chỉ có 1 đơn, không phân trang.
     */
    @EntityGraph(attributePaths = "items")
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    /**
     * Danh sách đơn có phân trang: KHÔNG join items (xem lý do ở BookRepository).
     * Items của cả trang được tải theo lô nhờ hibernate.default_batch_fetch_size.
     */
    Page<Order> findByUserId(Long userId, Pageable pageable);

    // ===================== Dành cho admin và job =====================

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(Long id);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * Id của các đơn PENDING tạo trước mốc cutoff, cũ nhất trước. Chỉ lấy id (không
     * tải cả entity) vì mỗi đơn sẽ được xử lý lại trong transaction riêng của nó.
     * Pageable dùng để giới hạn số đơn mỗi lần quét (LIMIT).
     */
    @Query("""
            select o.id from Order o
             where o.status = com.nguyenduyhung.bookstore.order.OrderStatus.PENDING
               and o.createdAt < :cutoff
             order by o.createdAt, o.id
            """)
    List<Long> findExpiredPendingOrderIds(@Param("cutoff") Instant cutoff, Pageable pageable);
}
