package com.nguyenduyhung.bookstore.order;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bảng chuyển trạng thái viết TƯỜNG MINH từng dòng, độc lập với code trong enum.
 * Nếu ai đó lỡ sửa luật trong OrderStatus, bảng này sẽ báo ngay.
 */
class OrderStatusTest {

    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @CsvSource({
            // Được phép
            "PENDING,   PAID,      true",
            "PENDING,   CANCELLED, true",
            "PAID,      SHIPPING,  true",
            "PAID,      CANCELLED, true",
            "SHIPPING,  COMPLETED, true",
            // Không được phép: nhảy cóc, đi lùi, hủy khi đã giao, rời trạng thái cuối
            "PENDING,   SHIPPING,  false",
            "PENDING,   COMPLETED, false",
            "PAID,      PENDING,   false",
            "PAID,      COMPLETED, false",
            "SHIPPING,  CANCELLED, false",
            "SHIPPING,  PAID,      false",
            "COMPLETED, CANCELLED, false",
            "COMPLETED, SHIPPING,  false",
            "CANCELLED, PENDING,   false",
            "CANCELLED, PAID,      false",
            // Không được "chuyển" sang chính nó
            "PAID,      PAID,      false"
    })
    void canTransitionTo(OrderStatus from, OrderStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }

    /** Doanh thu = đã trả tiền và chưa hủy. */
    @ParameterizedTest(name = "{0} tính vào doanh thu: {1}")
    @CsvSource({
            "PENDING,   false",
            "PAID,      true",
            "SHIPPING,  true",
            "COMPLETED, true",
            "CANCELLED, false"
    })
    void countsAsRevenue(OrderStatus status, boolean expected) {
        assertThat(status.countsAsRevenue()).isEqualTo(expected);
    }
}
