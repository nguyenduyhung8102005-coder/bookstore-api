package com.nguyenduyhung.bookstore.order;

import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import com.nguyenduyhung.bookstore.common.response.PageResponse;
import com.nguyenduyhung.bookstore.order.dto.OrderResponse;
import com.nguyenduyhung.bookstore.order.dto.OrderSummaryResponse;
import com.nguyenduyhung.bookstore.order.dto.PlaceOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    static final int MAX_PLACEMENT_ATTEMPTS = 3;

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final OrderPlacementService orderPlacementService;
    private final OrderRepository orderRepository;

    /**
     * Đặt hàng, tự thử lại khi va chạm với request khác đang mua cùng sách.
     *
     * Cách optimistic locking xử lý "2 người mua cuốn cuối cùng":
     *   1. A và B cùng đọc: tồn kho 1, version 7.
     *   2. A commit trước: UPDATE books SET stock = 0, version = 8 WHERE id = ? AND version = 7 -> 1 dòng.
     *   3. B commit sau: cùng câu UPDATE ... AND version = 7 -> 0 dòng (version đã là 8)
     *      -> Hibernate ném ObjectOptimisticLockingFailureException, transaction của B rollback.
     *   4. B được thử lại TỪ ĐẦU: đọc lại thấy tồn kho 0 -> lỗi INSUFFICIENT_STOCK rõ ràng.
     *
     * Vì sao method này KHÔNG có @Transactional và phải gọi sang một bean khác?
     * - Mỗi lần thử phải là một transaction MỚI: transaction cũ đã hỏng (rollback-only),
     *   và chỉ transaction mới mới đọc được dữ liệu mới nhất (tồn kho đã bị A trừ).
     * - @Transactional hoạt động qua proxy của Spring. Nếu gọi placeOrder của chính
     *   class này (self-invocation), lời gọi không đi qua proxy và KHÔNG có transaction
     *   mới nào được mở. Vì vậy phần transaction nằm ở bean riêng OrderPlacementService.
     *
     * Bắt ConcurrencyFailureException (lớp cha của cả optimistic lock failure lẫn
     * deadlock) vì cả hai đều là lỗi tạm thời: thử lại thường sẽ thành công.
     */
    public OrderResponse placeOrder(Long userId, PlaceOrderRequest request) {
        for (int attempt = 1; ; attempt++) {
            try {
                return orderPlacementService.placeOrder(userId, request);
            } catch (ConcurrencyFailureException e) {
                if (attempt >= MAX_PLACEMENT_ATTEMPTS) {
                    log.warn("Placing order for user {} failed after {} attempts due to concurrent updates",
                            userId, attempt);
                    throw new AppException(ErrorCode.CONCURRENT_UPDATE,
                            "Too many people are buying the same books right now, please try again");
                }
                log.info("Concurrent update while placing order for user {} (attempt {}), retrying", userId, attempt);
                backOff(attempt);
            }
        }
    }

    /** Danh sách đơn của chính user, mới nhất trước (client không tự chọn cách sắp xếp). */
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getMyOrders(Long userId, Pageable pageable) {
        Pageable newestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST);
        return PageResponse.from(orderRepository.findByUserId(userId, newestFirst).map(OrderMapper::toSummary));
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .map(OrderMapper::toResponse)
                .orElseThrow(() -> AppException.notFound("Order", orderId));
    }

    /**
     * Chờ một khoảng NGẪU NHIÊN, tăng dần theo số lần thử, trước khi thử lại. Nếu mọi
     * request cùng thử lại ngay lập tức, chúng lại va chạm đúng như lần trước;
     * thời gian chờ ngẫu nhiên (jitter) giúp chúng "tản" ra.
     */
    private static void backOff(int attempt) {
        long delayMillis = ThreadLocalRandom.current().nextLong(10, 50) * attempt;
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            // Khôi phục cờ interrupt để code phía trên biết luồng đã bị yêu cầu dừng.
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.CONCURRENT_UPDATE);
        }
    }
}
