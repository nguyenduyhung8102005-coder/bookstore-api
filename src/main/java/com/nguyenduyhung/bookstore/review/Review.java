package com.nguyenduyhung.bookstore.review;

import com.nguyenduyhung.bookstore.common.entity.BaseEntity;
import com.nguyenduyhung.bookstore.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Một đánh giá của người dùng cho một cuốn sách.
 *
 * Khác với CartItem và Order (lưu "Long userId"), ở đây user là một quan hệ
 * @ManyToOne thật, vì danh sách đánh giá CẦN hiển thị tên người viết. Ngược lại,
 * sách chỉ cần id: đánh giá luôn được truy vấn theo bookId, không cần đọc thông tin sách.
 */
@Getter
@Entity
@Table(name = "reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "text")
    private String comment;

    public static Review create(User user, Long bookId, int rating, String comment) {
        Review review = new Review();
        review.user = user;
        review.bookId = bookId;
        review.edit(rating, comment);
        return review;
    }

    public void edit(int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5: " + rating);
        }
        this.rating = rating;
        // Nhận xét chỉ có khoảng trắng thì coi như không có nhận xét.
        this.comment = (comment == null || comment.isBlank()) ? null : comment.trim();
    }

    /**
     * user.getId() trên một proxy LAZY KHÔNG làm Hibernate truy vấn bảng users:
     * id đã có sẵn trong proxy (chính là giá trị cột user_id).
     */
    public boolean isWrittenBy(Long userId) {
        return user.getId().equals(userId);
    }
}
