package com.nguyenduyhung.bookstore.cart;

import com.nguyenduyhung.bookstore.cart.dto.CartResponse;
import com.nguyenduyhung.bookstore.catalog.book.Book;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.nguyenduyhung.bookstore.cart.CartTestData.book;
import static org.assertj.core.api.Assertions.assertThat;

class CartMapperTest {

    @Test
    void totals_areComputedExactly() {
        var items = List.of(
                CartItem.create(1L, book(10L, "Dế Mèn", "75000.00", 10), 2),
                CartItem.create(1L, book(11L, "Số đỏ", "120000.50", 10), 1));

        CartResponse cart = CartMapper.toResponse(items);

        assertThat(cart.totalItems()).isEqualTo(3);
        // isEqualByComparingTo dùng compareTo: so GIÁ TRỊ, bỏ qua khác biệt về scale (270000.5 vs 270000.50)
        assertThat(cart.totalAmount()).isEqualByComparingTo("270000.50");
        assertThat(cart.items()).extracting(line -> line.lineTotal().toPlainString())
                .containsExactly("150000.00", "120000.50");
        assertThat(cart.hasUnavailableItems()).isFalse();
    }

    @Test
    void emptyCart_hasZeroTotalWithTwoDecimals() {
        CartResponse cart = CartMapper.toResponse(List.of());

        assertThat(cart.items()).isEmpty();
        assertThat(cart.totalAmount()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void itemIsUnavailable_whenStockIsLowerThanQuantity() {
        var cart = CartMapper.toResponse(List.of(CartItem.create(1L, book(10L, "Dế Mèn", "75000.00", 1), 2)));

        assertThat(cart.items().get(0).available()).isFalse();
        assertThat(cart.hasUnavailableItems()).isTrue();
    }

    @Test
    void itemIsUnavailable_whenBookIsDeactivated() {
        Book book = book(10L, "Dế Mèn", "75000.00", 10);
        book.deactivate();

        var cart = CartMapper.toResponse(List.of(CartItem.create(1L, book, 1)));

        assertThat(cart.items().get(0).available()).isFalse();
    }
}
