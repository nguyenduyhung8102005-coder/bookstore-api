package com.nguyenduyhung.bookstore.catalog.book;

import com.nguyenduyhung.bookstore.catalog.category.Category;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookStockTest {

    private final Book book = Book.create("Dế Mèn", "9780000000001", null, new BigDecimal("75000.00"), 3, 1941,
            Category.create("Thiếu nhi"), Set.of());

    @Test
    void decreaseStock_reducesQuantity() {
        book.decreaseStock(2);

        assertThat(book.getStockQuantity()).isEqualTo(1);
    }

    @Test
    void decreaseStock_canSellTheLastCopy() {
        book.decreaseStock(3);

        assertThat(book.getStockQuantity()).isZero();
    }

    @Test
    void decreaseStock_neverGoesBelowZero() {
        assertThatThrownBy(() -> book.decreaseStock(4)).isInstanceOf(IllegalArgumentException.class);
        assertThat(book.getStockQuantity()).isEqualTo(3);
    }
}
