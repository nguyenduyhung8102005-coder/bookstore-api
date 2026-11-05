package com.nguyenduyhung.bookstore.order;

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

import java.math.BigDecimal;

/**
 * Một dòng của đơn hàng.
 *
 * Sách được lưu bằng "Long bookId" + bản chép (snapshot) tên và giá, KHÔNG phải
 * quan hệ @ManyToOne Book. Đơn hàng là hồ sơ lịch sử: nó phải hiển thị đúng như
 * lúc mua, không phụ thuộc vào trạng thái hiện tại của sách (đổi giá, đổi tên,
 * ngừng bán). bookId vẫn giữ để liên kết về trang sách và để thống kê.
 */
@Getter
@Entity
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "book_title", nullable = false)
    private String bookTitle;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    /** Chỉ Order được tạo dòng của nó (package-private), để tổng tiền luôn được tính lại. */
    static OrderItem create(Order order, Long bookId, String bookTitle, BigDecimal unitPrice, int quantity) {
        OrderItem item = new OrderItem();
        item.order = order;
        item.bookId = bookId;
        item.bookTitle = bookTitle;
        item.unitPrice = unitPrice;
        item.quantity = quantity;
        return item;
    }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
