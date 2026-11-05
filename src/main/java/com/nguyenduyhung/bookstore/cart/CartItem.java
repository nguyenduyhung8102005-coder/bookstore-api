package com.nguyenduyhung.bookstore.cart;

import com.nguyenduyhung.bookstore.catalog.book.Book;
import com.nguyenduyhung.bookstore.common.entity.BaseEntity;
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
 * Một dòng trong giỏ hàng: user X muốn mua N cuốn sách Y.
 *
 * Vì sao user là "Long userId" mà book là quan hệ @ManyToOne?
 * - Giỏ hàng luôn được truy vấn THEO user (WHERE user_id = ?), và id user đã có
 *   sẵn trong access token. Ta không bao giờ cần đọc thông tin User (tên, email)
 *   từ giỏ hàng, nên lưu id là đủ, vừa đơn giản vừa không phải tải entity User.
 * - Ngược lại, hiển thị giỏ CẦN tên sách, giá, tồn kho, nên book là một quan hệ thật.
 * Ràng buộc khóa ngoại trong DB vẫn đảm bảo user_id luôn hợp lệ.
 */
@Getter
@Entity
@Table(name = "cart_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends BaseEntity {

    /** Giới hạn số lượng một đầu sách trong giỏ, chặn những yêu cầu bất thường. */
    public static final int MAX_QUANTITY = 99;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private int quantity;

    public static CartItem create(Long userId, Book book, int quantity) {
        CartItem item = new CartItem();
        item.userId = userId;
        item.book = book;
        item.changeQuantity(quantity);
        return item;
    }

    public void changeQuantity(int quantity) {
        if (quantity <= 0) {
            // Service đã kiểm tra trước; đây là chốt chặn cuối để entity không
            // bao giờ rơi vào trạng thái sai, dù được gọi từ đâu.
            throw new IllegalArgumentException("Quantity must be positive: " + quantity);
        }
        this.quantity = quantity;
    }
}
