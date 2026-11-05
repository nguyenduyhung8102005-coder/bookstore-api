package com.nguyenduyhung.bookstore.common.concurrency;

import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Chạy lại một thao tác khi nó thất bại vì XUNG ĐỘT TẠM THỜI với request khác
 * (optimistic lock, deadlock). Tách ra từ OrderService ở GĐ 5a để dùng chung cho
 * đặt hàng, thanh toán, hủy đơn, admin đổi trạng thái và job hết hạn.
 *
 * QUAN TRỌNG khi dùng: action phải gọi sang một method @Transactional của BEAN
 * KHÁC, để mỗi lần thử là một transaction mới (xem giải thích ở OrderService).
 * Bản thân class này không mở transaction.
 *
 * Phương án khác: thư viện Spring Retry (@Retryable), hoặc @Retryable có sẵn từ
 * Spring Framework 7. Ở đây tự viết để thấy rõ cơ chế.
 */
@Slf4j
@Component
public class ConcurrencyRetryTemplate {

    public static final int MAX_ATTEMPTS = 3;

    public <T> T execute(String operation, Supplier<T> action) {
        for (int attempt = 1; ; attempt++) {
            try {
                return action.get();
            } catch (ConcurrencyFailureException e) {
                if (attempt >= MAX_ATTEMPTS) {
                    log.warn("'{}' failed after {} attempts due to concurrent updates", operation, attempt);
                    throw new AppException(ErrorCode.CONCURRENT_UPDATE,
                            "The data is being changed by other requests, please try again");
                }
                log.info("Concurrent update during '{}' (attempt {}), retrying", operation, attempt);
                backOff(attempt);
            }
        }
    }

    /**
     * Chờ một khoảng NGẪU NHIÊN, tăng dần theo số lần thử. Nếu mọi request cùng
     * thử lại ngay lập tức, chúng lại va chạm đúng như lần trước; thời gian chờ
     * ngẫu nhiên (jitter) giúp chúng "tản" ra.
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
