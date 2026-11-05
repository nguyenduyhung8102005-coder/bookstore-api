package com.nguyenduyhung.bookstore.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** Kèm tên người viết trong cùng câu SQL (JOIN user là quan hệ *-to-one nên an toàn khi phân trang). */
    @EntityGraph(attributePaths = "user")
    Page<Review> findByBookId(Long bookId, Pageable pageable);

    /** "UserId": Review không có thuộc tính userId, nên Spring Data hiểu là user.id. */
    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    /**
     * "select new ..." (constructor expression): JPQL tạo thẳng record RatingSummary
     * từ kết quả AVG và COUNT, không phải tải entity nào.
     */
    @Query("""
            select new com.nguyenduyhung.bookstore.review.RatingSummary(avg(r.rating), count(r))
              from Review r
             where r.bookId = :bookId
            """)
    RatingSummary getRatingSummary(@Param("bookId") Long bookId);
}
