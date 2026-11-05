package com.nguyenduyhung.bookstore.order;

import com.nguyenduyhung.bookstore.catalog.book.Book;
import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import com.nguyenduyhung.bookstore.order.dto.OrderResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.List;

import static com.nguyenduyhung.bookstore.order.OrderTestData.SHIPPING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiểm tra riêng logic THỬ LẠI: OrderPlacementService được mock để giả lập
 * "va chạm optimistic lock" bao nhiêu lần tùy ý, không cần nhiều luồng thật.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long USER_ID = 1L;
    private static final OrderResponse PLACED_ORDER = new OrderResponse(
            100L, OrderStatus.PENDING, new BigDecimal("75000.00"), "An", "0912345678", "Hà Nội",
            List.of(), null, null, null);

    @Mock
    private OrderPlacementService orderPlacementService;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void conflictTwice_thenSuccess_returnsOrderOnThirdAttempt() {
        when(orderPlacementService.placeOrder(USER_ID, SHIPPING))
                .thenThrow(optimisticLockFailure())
                .thenThrow(optimisticLockFailure())
                .thenReturn(PLACED_ORDER);

        assertThat(orderService.placeOrder(USER_ID, SHIPPING)).isEqualTo(PLACED_ORDER);
        verify(orderPlacementService, times(3)).placeOrder(USER_ID, SHIPPING);
    }

    @Test
    void conflictEveryTime_givesUpAfterMaxAttempts() {
        when(orderPlacementService.placeOrder(USER_ID, SHIPPING)).thenThrow(optimisticLockFailure());

        assertThatThrownBy(() -> orderService.placeOrder(USER_ID, SHIPPING))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CONCURRENT_UPDATE);
        verify(orderPlacementService, times(OrderService.MAX_PLACEMENT_ATTEMPTS)).placeOrder(USER_ID, SHIPPING);
    }

    /** Lỗi nghiệp vụ (hết hàng) thì thử lại cũng vô ích: phải báo ngay, không retry. */
    @Test
    void businessError_isNotRetried() {
        when(orderPlacementService.placeOrder(USER_ID, SHIPPING))
                .thenThrow(new AppException(ErrorCode.INSUFFICIENT_STOCK));

        assertThatThrownBy(() -> orderService.placeOrder(USER_ID, SHIPPING))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
        verify(orderPlacementService, times(1)).placeOrder(USER_ID, SHIPPING);
    }

    private static ObjectOptimisticLockingFailureException optimisticLockFailure() {
        return new ObjectOptimisticLockingFailureException(Book.class, 1L);
    }
}
