package com.nguyenduyhung.bookstore.order;

import com.nguyenduyhung.bookstore.catalog.book.Book;
import com.nguyenduyhung.bookstore.catalog.book.BookRepository;
import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.nguyenduyhung.bookstore.order.OrderTestData.book;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderLifecycleServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ORDER_ID = 100L;
    private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private BookRepository bookRepository;

    private OrderLifecycleService service;

    @BeforeEach
    void setUp() {
        // Clock cố định: "bây giờ" luôn là NOW, nên test về hết hạn cho kết quả xác định.
        service = new OrderLifecycleService(orderRepository, bookRepository,
                new OrderProperties(Duration.ofMinutes(30)), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // ===================== Thanh toán =====================

    @Test
    void pay_pendingOrderWithinTimeout_becomesPaid() {
        Order order = pendingOrderCreatedAt(NOW.minus(Duration.ofMinutes(5)));
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        service.pay(USER_ID, ORDER_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaidAt()).isEqualTo(NOW);
    }

    @Test
    void pay_orderOlderThanTimeout_isRejectedEvenIfJobHasNotRunYet() {
        Order order = pendingOrderCreatedAt(NOW.minus(Duration.ofMinutes(31)));
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.pay(USER_ID, ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ORDER_EXPIRED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    // ===================== Hủy =====================

    @Test
    void cancel_restoresStockOfEveryItem() {
        Order order = pendingOrderCreatedAt(NOW.minus(Duration.ofMinutes(5)));
        order.addItem(1L, "Dế Mèn", new BigDecimal("75000.00"), 2);
        order.addItem(2L, "Số đỏ", new BigDecimal("120000.00"), 1);
        Book deMen = book(1L, "Dế Mèn", "75000.00", 3);
        Book soDo = book(2L, "Số đỏ", "120000.00", 4);
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(bookRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(deMen, soDo));

        service.cancelByUser(USER_ID, ORDER_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isEqualTo(NOW);
        assertThat(deMen.getStockQuantity()).isEqualTo(5);
        assertThat(soDo.getStockQuantity()).isEqualTo(5);
    }

    /** Luật chuyển trạng thái được kiểm tra TRƯỚC khi hoàn kho: hủy sai thì kho không đổi. */
    @Test
    void cancel_shippingOrder_isRejected_withoutTouchingStock() {
        Order order = pendingOrderCreatedAt(NOW.minus(Duration.ofMinutes(5)));
        order.markPaid(NOW);
        order.ship();
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancelByUser(USER_ID, ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        verify(bookRepository, never()).findAllById(anyIterable());
    }

    // ===================== Admin =====================

    @Test
    void admin_cannotMarkOrderAsPaid() {
        assertThatThrownBy(() -> service.changeStatusByAdmin(ORDER_ID, OrderStatus.PAID))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_PARAMETER);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void admin_shipsPaidOrder() {
        Order order = pendingOrderCreatedAt(NOW.minus(Duration.ofMinutes(5)));
        order.markPaid(NOW);
        when(orderRepository.findWithItemsById(ORDER_ID)).thenReturn(Optional.of(order));

        service.changeStatusByAdmin(ORDER_ID, OrderStatus.SHIPPING);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPING);
    }

    // ===================== Job hết hạn =====================

    /** Người mua vừa thanh toán xong ngay trước khi job xử lý tới đơn này: bỏ qua, không hủy. */
    @Test
    void expire_orderAlreadyPaid_isSkipped() {
        Order order = pendingOrderCreatedAt(NOW.minus(Duration.ofMinutes(40)));
        order.markPaid(NOW);
        when(orderRepository.findWithItemsById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThat(service.expire(ORDER_ID)).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(bookRepository, never()).findAllById(anyIterable());
    }

    @Test
    void expire_pendingOrderPastTimeout_isCancelled() {
        Order order = pendingOrderCreatedAt(NOW.minus(Duration.ofMinutes(31)));
        order.addItem(1L, "Dế Mèn", new BigDecimal("75000.00"), 1);
        Book deMen = book(1L, "Dế Mèn", "75000.00", 0);
        when(orderRepository.findWithItemsById(ORDER_ID)).thenReturn(Optional.of(order));
        when(bookRepository.findAllById(List.of(1L))).thenReturn(List.of(deMen));

        assertThat(service.expire(ORDER_ID)).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(deMen.getStockQuantity()).isEqualTo(1);
    }

    private static Order pendingOrderCreatedAt(Instant createdAt) {
        Order order = Order.create(USER_ID, "An", "0912345678", "Hà Nội");
        // createdAt bình thường do JPA Auditing gán khi lưu; ở đây không có DB nên gán tay.
        ReflectionTestUtils.setField(order, "createdAt", createdAt);
        ReflectionTestUtils.setField(order, "id", ORDER_ID);
        return order;
    }
}
