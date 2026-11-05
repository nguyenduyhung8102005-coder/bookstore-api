package com.nguyenduyhung.bookstore.common.concurrency;

import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm tra riêng logic THỬ LẠI (chuyển từ OrderServiceTest ở GĐ 5a sang đây khi
 * logic này được tách thành class dùng chung). Giả lập "va chạm" bằng cách ném
 * exception bao nhiêu lần tùy ý, không cần nhiều luồng thật.
 */
class ConcurrencyRetryTemplateTest {

    private final ConcurrencyRetryTemplate retryTemplate = new ConcurrencyRetryTemplate();

    @Test
    void conflictTwice_thenSuccess_returnsResultOnThirdAttempt() {
        AtomicInteger attempts = new AtomicInteger();

        String result = retryTemplate.execute("test", () -> {
            if (attempts.incrementAndGet() < 3) {
                throw new ObjectOptimisticLockingFailureException("Book", 1L);
            }
            return "done";
        });

        assertThat(result).isEqualTo("done");
        assertThat(attempts).hasValue(3);
    }

    @Test
    void conflictEveryTime_givesUpAfterMaxAttempts() {
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> retryTemplate.execute("test", () -> {
            attempts.incrementAndGet();
            throw new ObjectOptimisticLockingFailureException("Book", 1L);
        }))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CONCURRENT_UPDATE);
        assertThat(attempts).hasValue(ConcurrencyRetryTemplate.MAX_ATTEMPTS);
    }

    /** Deadlock cũng là lỗi tạm thời (lớp con của ConcurrencyFailureException), nên được thử lại. */
    @Test
    void deadlock_isAlsoRetried() {
        AtomicInteger attempts = new AtomicInteger();

        String result = retryTemplate.execute("test", () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new CannotAcquireLockException("deadlock detected");
            }
            return "done";
        });

        assertThat(result).isEqualTo("done");
        assertThat(attempts).hasValue(2);
    }

    /** Lỗi nghiệp vụ (hết hàng...) thì thử lại cũng vô ích: phải báo ngay. */
    @Test
    void businessError_isNotRetried() {
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> retryTemplate.execute("test", () -> {
            attempts.incrementAndGet();
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
        assertThat(attempts).hasValue(1);
    }
}
