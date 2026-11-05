package com.nguyenduyhung.bookstore.order;

import com.nguyenduyhung.bookstore.catalog.book.Book;
import com.nguyenduyhung.bookstore.catalog.book.BookRepository;
import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import com.nguyenduyhung.bookstore.order.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Các thao tác làm ĐỔI TRẠNG THÁI đơn hàng sau khi đã tạo. Mỗi method là MỘT
 * transaction; việc thử lại khi va chạm nằm ở OrderService (bên ngoài transaction),
 * giống hệt cách làm với đặt hàng ở GĐ 5a.
 *
 * Vì sao những thao tác này cũng cần thử lại?
 * - Hủy đơn phải HOÀN KHO: sửa bảng books, có thể va chạm với người đang đặt mua
 *   cùng cuốn sách đó.
 * - Đơn hàng có @Version: user bấm hủy đúng lúc admin chuyển sang SHIPPING thì một
 *   bên sẽ va chạm. Thử lại sẽ đọc trạng thái MỚI và nhận lỗi nghiệp vụ đúng
 *   ("không thể hủy đơn đang giao") thay vì ghi đè lên thao tác của admin.
 */
@Service
@RequiredArgsConstructor
public class OrderLifecycleService {

    /** Admin chỉ được đặt các trạng thái này; PAID chỉ do người mua thanh toán. */
    static final Set<OrderStatus> ADMIN_SETTABLE_STATUSES =
            Set.of(OrderStatus.SHIPPING, OrderStatus.COMPLETED, OrderStatus.CANCELLED);

    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final OrderProperties orderProperties;
    private final Clock clock;

    // ===================== Người mua =====================

    /**
     * Giả lập thanh toán thành công. Job hủy đơn quá hạn chạy định kỳ (mỗi phút),
     * nên có một khoảng thời gian đơn đã quá hạn mà job chưa kịp hủy; kiểm tra ở
     * đây đảm bảo không ai thanh toán được đơn đã quá hạn trong khoảng đó.
     */
    @Transactional
    public OrderResponse pay(Long userId, Long orderId) {
        Order order = getOwnOrder(userId, orderId);
        Instant now = clock.instant();
        if (order.isPending() && isExpired(order, now)) {
            throw new AppException(ErrorCode.ORDER_EXPIRED);
        }
        order.markPaid(now);
        return OrderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse cancelByUser(Long userId, Long orderId) {
        Order order = getOwnOrder(userId, orderId);
        cancelAndRestock(order);
        return OrderMapper.toResponse(order);
    }

    // ===================== Admin =====================

    @Transactional
    public OrderResponse changeStatusByAdmin(Long orderId, OrderStatus target) {
        if (!ADMIN_SETTABLE_STATUSES.contains(target)) {
            throw new AppException(ErrorCode.INVALID_PARAMETER,
                    "Admin can only change order status to SHIPPING, COMPLETED or CANCELLED");
        }
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> AppException.notFound("Order", orderId));
        switch (target) {
            case SHIPPING -> order.ship();
            case COMPLETED -> order.complete();
            case CANCELLED -> cancelAndRestock(order);
            default -> throw new IllegalStateException("Unexpected status: " + target);
        }
        return OrderMapper.toResponse(order);
    }

    // ===================== Job =====================

    /**
     * Hủy MỘT đơn quá hạn. Phải kiểm tra lại trạng thái ở đây: từ lúc job lấy danh
     * sách id đến lúc xử lý tới đơn này, người mua có thể vừa thanh toán xong.
     *
     * @return true nếu đơn bị hủy, false nếu đơn không còn cần hủy (bỏ qua)
     */
    @Transactional
    public boolean expire(Long orderId) {
        Order order = orderRepository.findWithItemsById(orderId).orElse(null);
        if (order == null || !order.isPending() || !isExpired(order, clock.instant())) {
            return false;
        }
        cancelAndRestock(order);
        return true;
    }

    // ===================== Helper =====================

    private Order getOwnOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> AppException.notFound("Order", orderId));
    }

    private boolean isExpired(Order order, Instant now) {
        return order.getCreatedAt().isBefore(now.minus(orderProperties.pendingTimeout()));
    }

    /**
     * Hủy đơn (kiểm tra luật chuyển trạng thái trước) rồi trả lại kho cho từng sách.
     * Tải mọi sách bằng MỘT câu "WHERE id IN (...)", và cộng kho theo id tăng dần
     * (cùng thứ tự với lúc đặt hàng) để tránh deadlock.
     */
    private void cancelAndRestock(Order order) {
        order.cancel(clock.instant());

        List<Long> bookIds = order.getItems().stream().map(OrderItem::getBookId).toList();
        Map<Long, Book> booksById = bookRepository.findAllById(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));

        order.getItems().stream()
                .sorted(Comparator.comparing(OrderItem::getBookId))
                .forEach(item -> booksById.get(item.getBookId()).increaseStock(item.getQuantity()));
    }
}
