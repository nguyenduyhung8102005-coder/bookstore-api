package com.nguyenduyhung.bookstore.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Định kỳ hủy các đơn PENDING quá hạn chưa thanh toán và hoàn kho.
 *
 * Thiết kế:
 * - Lấy danh sách id trước, rồi xử lý TỪNG đơn trong transaction RIÊNG (qua
 *   OrderService, có thử lại khi va chạm). Một đơn lỗi không làm hỏng cả lô, và
 *   không giữ một transaction dài khóa nhiều dòng cùng lúc.
 * - Mỗi lần chỉ xử lý tối đa BATCH_SIZE đơn; còn thì lần chạy sau xử lý tiếp.
 * - fixedDelay: lần chạy sau bắt đầu SAU KHI lần trước kết thúc + khoảng chờ, nên
 *   hai lần chạy không bao giờ chồng lên nhau (khác với fixedRate).
 *
 * Giới hạn: khi chạy NHIỀU instance ứng dụng, mỗi instance đều chạy job này. Kết
 * quả vẫn đúng (optimistic lock + kiểm tra lại trạng thái) nhưng lãng phí. Giải
 * pháp thường dùng là ShedLock: dùng một bảng trong DB làm khóa để chỉ một
 * instance chạy job tại mỗi thời điểm.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpirationJob {

    static final int BATCH_SIZE = 100;

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final OrderProperties orderProperties;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.order.expiration-check-interval:PT1M}")
    public void cancelExpiredOrders() {
        Instant cutoff = clock.instant().minus(orderProperties.pendingTimeout());
        List<Long> orderIds = orderRepository.findExpiredPendingOrderIds(cutoff, PageRequest.of(0, BATCH_SIZE));

        int cancelled = 0;
        for (Long orderId : orderIds) {
            try {
                if (orderService.expire(orderId)) {
                    cancelled++;
                }
            } catch (RuntimeException e) {
                // Ghi log rồi xử lý đơn tiếp theo; đơn lỗi sẽ được thử lại ở lần chạy sau.
                log.warn("Could not expire order {}: {}", orderId, e.getMessage());
            }
        }
        if (cancelled > 0) {
            log.info("Cancelled {} expired pending order(s)", cancelled);
        }
    }
}
