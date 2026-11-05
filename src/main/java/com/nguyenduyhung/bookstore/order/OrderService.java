package com.nguyenduyhung.bookstore.order;

import com.nguyenduyhung.bookstore.common.concurrency.ConcurrencyRetryTemplate;
import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.response.PageResponse;
import com.nguyenduyhung.bookstore.order.dto.AdminOrderSummaryResponse;
import com.nguyenduyhung.bookstore.order.dto.OrderResponse;
import com.nguyenduyhung.bookstore.order.dto.OrderSummaryResponse;
import com.nguyenduyhung.bookstore.order.dto.PlaceOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Cửa trước" của module đơn hàng, được Controller và Job gọi.
 *
 * Các thao tác GHI (đặt hàng, thanh toán, hủy, đổi trạng thái) KHÔNG có
 * @Transactional ở đây: chúng được bọc trong ConcurrencyRetryTemplate, và mỗi lần
 * thử gọi sang một bean khác (OrderPlacementService / OrderLifecycleService) có
 * @Transactional, nên mỗi lần thử là MỘT transaction MỚI.
 *
 * Nhắc lại cách optimistic locking xử lý "2 người mua cuốn cuối cùng":
 *   1. A và B cùng đọc: tồn kho 1, version 7.
 *   2. A commit trước: UPDATE books SET stock = 0, version = 8 WHERE id = ? AND version = 7 -> 1 dòng.
 *   3. B commit sau: cùng câu UPDATE ... AND version = 7 -> 0 dòng (version đã là 8)
 *      -> Hibernate ném ObjectOptimisticLockingFailureException, transaction của B rollback.
 *   4. B được thử lại TỪ ĐẦU trong transaction mới: đọc lại thấy tồn kho 0 -> INSUFFICIENT_STOCK.
 *
 * Vì sao phải gọi sang bean khác? @Transactional hoạt động qua proxy của Spring.
 * Gọi một method trong CÙNG class (self-invocation) không đi qua proxy, nên sẽ
 * không có transaction mới nào được mở cho lần thử lại.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final OrderPlacementService orderPlacementService;
    private final OrderLifecycleService orderLifecycleService;
    private final OrderRepository orderRepository;
    private final ConcurrencyRetryTemplate retryTemplate;

    // ===================== Ghi (có thử lại khi va chạm) =====================

    public OrderResponse placeOrder(Long userId, PlaceOrderRequest request) {
        return retryTemplate.execute("place order",
                () -> orderPlacementService.placeOrder(userId, request));
    }

    public OrderResponse pay(Long userId, Long orderId) {
        return retryTemplate.execute("pay order",
                () -> orderLifecycleService.pay(userId, orderId));
    }

    public OrderResponse cancel(Long userId, Long orderId) {
        return retryTemplate.execute("cancel order",
                () -> orderLifecycleService.cancelByUser(userId, orderId));
    }

    public OrderResponse changeStatusByAdmin(Long orderId, OrderStatus target) {
        return retryTemplate.execute("change order status",
                () -> orderLifecycleService.changeStatusByAdmin(orderId, target));
    }

    /** Dùng cho job: hủy một đơn quá hạn nếu nó vẫn còn PENDING. */
    public boolean expire(Long orderId) {
        return retryTemplate.execute("expire order", () -> orderLifecycleService.expire(orderId));
    }

    // ===================== Đọc =====================

    /** Danh sách đơn của chính user, mới nhất trước (client không tự chọn cách sắp xếp). */
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getMyOrders(Long userId, Pageable pageable) {
        return PageResponse.from(orderRepository.findByUserId(userId, newestFirst(pageable))
                .map(OrderMapper::toSummary));
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .map(OrderMapper::toResponse)
                .orElseThrow(() -> AppException.notFound("Order", orderId));
    }

    /** Admin xem mọi đơn, lọc theo trạng thái nếu có. */
    @Transactional(readOnly = true)
    public PageResponse<AdminOrderSummaryResponse> searchForAdmin(OrderStatus status, Pageable pageable) {
        Pageable sorted = newestFirst(pageable);
        Page<Order> orders = status == null
                ? orderRepository.findAll(sorted)
                : orderRepository.findByStatus(status, sorted);
        return PageResponse.from(orders.map(OrderMapper::toAdminSummary));
    }

    @Transactional(readOnly = true)
    public OrderResponse getForAdmin(Long orderId) {
        return orderRepository.findWithItemsById(orderId)
                .map(OrderMapper::toResponse)
                .orElseThrow(() -> AppException.notFound("Order", orderId));
    }

    private static Pageable newestFirst(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST);
    }
}
