package com.nguyenduyhung.bookstore.cart;

import com.nguyenduyhung.bookstore.cart.dto.AddCartItemRequest;
import com.nguyenduyhung.bookstore.catalog.book.Book;
import com.nguyenduyhung.bookstore.catalog.book.BookRepository;
import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.nguyenduyhung.bookstore.cart.CartTestData.book;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long BOOK_ID = 10L;

    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void addItem_newBook_createsNewLine() {
        Book book = book(BOOK_ID, "Dế Mèn", "75000.00", 10);
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
        when(cartItemRepository.findByUserIdAndBookId(USER_ID, BOOK_ID)).thenReturn(Optional.empty());
        when(cartItemRepository.findByUserIdOrderByCreatedAtAscIdAsc(USER_ID)).thenReturn(List.of());

        cartService.addItem(USER_ID, new AddCartItemRequest(BOOK_ID, 2));

        ArgumentCaptor<CartItem> saved = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getValue().getQuantity()).isEqualTo(2);
    }

    @Test
    void addItem_bookAlreadyInCart_addsUpQuantityInsteadOfCreatingNewLine() {
        Book book = book(BOOK_ID, "Dế Mèn", "75000.00", 10);
        CartItem existing = CartItem.create(USER_ID, book, 2);
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
        when(cartItemRepository.findByUserIdAndBookId(USER_ID, BOOK_ID)).thenReturn(Optional.of(existing));
        when(cartItemRepository.findByUserIdOrderByCreatedAtAscIdAsc(USER_ID)).thenReturn(List.of(existing));

        cartService.addItem(USER_ID, new AddCartItemRequest(BOOK_ID, 3));

        assertThat(existing.getQuantity()).isEqualTo(5);
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void addItem_totalQuantityAboveStock_isRejected() {
        Book book = book(BOOK_ID, "Dế Mèn", "75000.00", 4);
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
        when(cartItemRepository.findByUserIdAndBookId(USER_ID, BOOK_ID))
                .thenReturn(Optional.of(CartItem.create(USER_ID, book, 3)));

        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddCartItemRequest(BOOK_ID, 2)))
                .isInstanceOf(AppException.class)
                .hasMessage("Only 4 copies of 'Dế Mèn' are left in stock")
                .extracting("errorCode").isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
    }

    @Test
    void addItem_totalQuantityAboveLimit_isRejected() {
        Book book = book(BOOK_ID, "Dế Mèn", "75000.00", 1_000);
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
        when(cartItemRepository.findByUserIdAndBookId(USER_ID, BOOK_ID))
                .thenReturn(Optional.of(CartItem.create(USER_ID, book, CartItem.MAX_QUANTITY)));

        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddCartItemRequest(BOOK_ID, 1)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.QUANTITY_LIMIT_EXCEEDED);
    }

    @Test
    void addItem_deactivatedBook_isRejected() {
        Book book = book(BOOK_ID, "Dế Mèn", "75000.00", 10);
        book.deactivate();
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddCartItemRequest(BOOK_ID, 1)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE);
    }

    @Test
    void updateQuantity_bookNotInCart_isRejected() {
        when(cartItemRepository.findByUserIdAndBookId(USER_ID, BOOK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateQuantity(USER_ID, BOOK_ID, 2))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND);
    }

    @Test
    void updateQuantity_setsNewValue() {
        CartItem item = CartItem.create(USER_ID, book(BOOK_ID, "Dế Mèn", "75000.00", 10), 2);
        when(cartItemRepository.findByUserIdAndBookId(USER_ID, BOOK_ID)).thenReturn(Optional.of(item));
        when(cartItemRepository.findByUserIdOrderByCreatedAtAscIdAsc(USER_ID)).thenReturn(List.of(item));

        cartService.updateQuantity(USER_ID, BOOK_ID, 7);

        assertThat(item.getQuantity()).isEqualTo(7);
    }
}
