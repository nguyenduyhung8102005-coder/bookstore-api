package com.nguyenduyhung.bookstore.order;

import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Test
    void newOrder_isPendingWithZeroTotal() {
        Order order = Order.create(1L, " An ", "0912345678", " Hà Nội ");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(order.getRecipientName()).isEqualTo("An");
    }

    @Test
    void addItem_recalculatesTotalAndItemCount() {
        Order order = Order.create(1L, "An", "0912345678", "Hà Nội");

        order.addItem(10L, "Dế Mèn", new BigDecimal("75000.00"), 2);
        order.addItem(11L, "Số đỏ", new BigDecimal("120000.50"), 1);

        assertThat(order.getTotalAmount()).isEqualByComparingTo("270000.50");
        assertThat(order.totalItems()).isEqualTo(3);
        assertThat(order.getItems()).extracting(OrderItem::getBookTitle).containsExactly("Dế Mèn", "Số đỏ");
    }

    @Test
    void markPaid_setsStatusAndPaidAt() {
        Order order = Order.create(1L, "An", "0912345678", "Hà Nội");
        Instant now = Instant.parse("2026-11-05T10:00:00Z");

        order.markPaid(now);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaidAt()).isEqualTo(now);
    }

    @Test
    void cancellingShippingOrder_isRejected_andStatusIsUnchanged() {
        Order order = Order.create(1L, "An", "0912345678", "Hà Nội");
        order.markPaid(Instant.parse("2026-11-05T10:00:00Z"));
        order.ship();

        assertThatThrownBy(() -> order.cancel(Instant.parse("2026-11-05T11:00:00Z")))
                .isInstanceOf(AppException.class)
                .hasMessage("Cannot change order status from SHIPPING to CANCELLED")
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPING);
        assertThat(order.getCancelledAt()).isNull();
    }

    @Test
    void items_cannotBeModifiedFromOutside() {
        Order order = Order.create(1L, "An", "0912345678", "Hà Nội");
        order.addItem(10L, "Dế Mèn", new BigDecimal("75000.00"), 1);

        assertThatThrownBy(() -> order.getItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
