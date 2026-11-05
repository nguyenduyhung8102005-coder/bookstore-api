package com.nguyenduyhung.bookstore.order;

import com.nguyenduyhung.bookstore.common.entity.BaseEntity;
import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Đơn hàng. Order là "gốc" (aggregate root) của các OrderItem: mọi thay đổi trên
 * dòng đơn hàng đều đi qua Order, để tổng tiền luôn khớp với các dòng.
 */
@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "shipping_address", nullable = false, length = 500)
    private String shippingAddress;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    private Long version;

    /*
     * cascade = ALL: lưu Order thì Hibernate tự lưu luôn các OrderItem mới.
     * orphanRemoval = true: bỏ một item khỏi danh sách thì dòng đó bị xóa khỏi DB.
     * mappedBy = "order": khóa ngoại nằm ở bảng order_items (phía OrderItem quản lý).
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public static Order create(Long userId, String recipientName, String phone, String shippingAddress) {
        Order order = new Order();
        order.userId = userId;
        order.status = OrderStatus.PENDING;
        order.recipientName = recipientName.trim();
        order.phone = phone.trim();
        order.shippingAddress = shippingAddress.trim();
        order.totalAmount = BigDecimal.ZERO.setScale(2);
        return order;
    }

    /** Thêm một dòng với tên và giá CHÉP tại thời điểm đặt hàng, rồi tính lại tổng tiền. */
    public void addItem(Long bookId, String bookTitle, BigDecimal unitPrice, int quantity) {
        items.add(OrderItem.create(this, bookId, bookTitle, unitPrice, quantity));
        totalAmount = items.stream()
                .map(OrderItem::lineTotal)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    /** Trả về danh sách CHỈ ĐỌC, để code bên ngoài không thể thêm/xóa item mà bỏ qua addItem(). */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public int totalItems() {
        return items.stream().mapToInt(OrderItem::getQuantity).sum();
    }

    // ===================== Chuyển trạng thái =====================
    // Mỗi method là một hành động nghiệp vụ có tên rõ ràng. Không có setStatus():
    // muốn đổi trạng thái thì BẮT BUỘC đi qua transitionTo(), nơi kiểm tra luật.

    public void markPaid(Instant now) {
        transitionTo(OrderStatus.PAID);
        this.paidAt = now;
    }

    public void ship() {
        transitionTo(OrderStatus.SHIPPING);
    }

    public void complete() {
        transitionTo(OrderStatus.COMPLETED);
    }

    /** Chỉ đổi trạng thái. Việc hoàn kho do service làm (cần truy cập bảng books). */
    public void cancel(Instant now) {
        transitionTo(OrderStatus.CANCELLED);
        this.cancelledAt = now;
    }

    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    private void transitionTo(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION,
                    "Cannot change order status from %s to %s".formatted(status, target));
        }
        this.status = target;
    }
}
