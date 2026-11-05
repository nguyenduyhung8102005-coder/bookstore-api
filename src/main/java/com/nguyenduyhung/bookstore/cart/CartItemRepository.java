package com.nguyenduyhung.bookstore.cart;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Toàn bộ giỏ của một user, kèm thông tin sách trong CÙNG một câu SQL (JOIN).
     * Giỏ không phân trang nên JOIN thẳng là an toàn, không bị N+1.
     * Thêm "IdAsc" sau "CreatedAtAsc" để thứ tự luôn cố định khi hai dòng tạo cùng lúc.
     */
    @EntityGraph(attributePaths = "book")
    List<CartItem> findByUserIdOrderByCreatedAtAscIdAsc(Long userId);

    /** "BookId": Spring Data hiểu là đi qua quan hệ book rồi lấy id (book.id). */
    Optional<CartItem> findByUserIdAndBookId(Long userId, Long bookId);

    /**
     * Xóa cả giỏ bằng MỘT câu DELETE. Nếu dùng deleteAll(list), Hibernate sẽ
     * tải từng dòng lên rồi xóa từng dòng một (N+1 câu).
     */
    @Modifying
    @Query("delete from CartItem c where c.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
