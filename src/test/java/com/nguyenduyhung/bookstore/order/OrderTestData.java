package com.nguyenduyhung.bookstore.order;

import com.nguyenduyhung.bookstore.catalog.book.Book;
import com.nguyenduyhung.bookstore.catalog.category.Category;
import com.nguyenduyhung.bookstore.order.dto.PlaceOrderRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Set;

/** Dữ liệu mẫu dùng chung cho các unit test của đơn hàng. */
final class OrderTestData {

    static final PlaceOrderRequest SHIPPING =
            new PlaceOrderRequest("Nguyễn Văn A", "0912345678", "1 Đại Cồ Việt, Hà Nội");

    private OrderTestData() {
    }

    static Book book(Long id, String title, String price, int stock) {
        Book book = Book.create(title, "978000000000" + id, null, new BigDecimal(price), stock, 2020,
                Category.create("Văn học"), Set.of());
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }
}
