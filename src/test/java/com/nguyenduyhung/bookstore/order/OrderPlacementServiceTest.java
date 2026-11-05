package com.nguyenduyhung.bookstore.order;

import com.nguyenduyhung.bookstore.cart.CartItem;
import com.nguyenduyhung.bookstore.cart.CartItemRepository;
import com.nguyenduyhung.bookstore.catalog.book.Book;
import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import com.nguyenduyhung.bookstore.order.dto.OrderResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.nguyenduyhung.bookstore.order.OrderTestData.SHIPPING;
import static com.nguyenduyhung.bookstore.order.OrderTestData.book;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPlacementServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderPlacementService orderPlacementService;

    @Test
    void emptyCart_isRejected() {
        when(cartItemRepository.findByUserIdOrderByCreatedAtAscIdAsc(USER_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> orderPlacementService.placeOrder(USER_ID, SHIPPING))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CART_EMPTY);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void success_decreasesStock_snapshotsPrices_savesOrder_andClearsCart() {
        Book soDo = book(2L, "Số đỏ", "120000.00", 5);
        Book deMen = book(1L, "Dế Mèn", "75000.00", 10);
        when(cartItemRepository.findByUserIdOrderByCreatedAtAscIdAsc(USER_ID)).thenReturn(List.of(
                CartItem.create(USER_ID, soDo, 1),
                CartItem.create(USER_ID, deMen, 2)));

        OrderResponse response = orderPlacementService.placeOrder(USER_ID, SHIPPING);

        assertThat(soDo.getStockQuantity()).isEqualTo(4);
        assertThat(deMen.getStockQuantity()).isEqualTo(8);
        assertThat(response.totalAmount()).isEqualByComparingTo("270000.00");
        // Các dòng được xử lý theo id sách tăng dần (chống deadlock), không theo thứ tự trong giỏ
        assertThat(response.items()).extracting(item -> item.bookId()).containsExactly(1L, 2L);

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(USER_ID);
        verify(cartItemRepository).deleteAllByUserId(USER_ID);
    }

    @Test
    void notEnoughStock_isRejected_andNothingIsSavedOrCleared() {
        Book deMen = book(1L, "Dế Mèn", "75000.00", 10);
        Book soDo = book(2L, "Số đỏ", "120000.00", 1);
        when(cartItemRepository.findByUserIdOrderByCreatedAtAscIdAsc(USER_ID)).thenReturn(List.of(
                CartItem.create(USER_ID, deMen, 1),
                CartItem.create(USER_ID, soDo, 2)));

        assertThatThrownBy(() -> orderPlacementService.placeOrder(USER_ID, SHIPPING))
                .isInstanceOf(AppException.class)
                .hasMessage("Only 1 copies of 'Số đỏ' are left in stock")
                .extracting("errorCode").isEqualTo(ErrorCode.INSUFFICIENT_STOCK);

        // Trong unit test không có DB nên không có rollback thật; ở đây chỉ kiểm tra
        // đơn không được lưu và giỏ không bị xóa. Rollback tồn kho được kiểm tra trong OrderIT.
        verify(orderRepository, never()).save(any(Order.class));
        verify(cartItemRepository, never()).deleteAllByUserId(anyLong());
    }

    @Test
    void deactivatedBook_isRejected() {
        Book deMen = book(1L, "Dế Mèn", "75000.00", 10);
        deMen.deactivate();
        when(cartItemRepository.findByUserIdOrderByCreatedAtAscIdAsc(USER_ID))
                .thenReturn(List.of(CartItem.create(USER_ID, deMen, 1)));

        assertThatThrownBy(() -> orderPlacementService.placeOrder(USER_ID, SHIPPING))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE);
    }
}
