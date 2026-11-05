package com.nguyenduyhung.bookstore.catalog.book;

import com.nguyenduyhung.bookstore.catalog.author.Author;
import com.nguyenduyhung.bookstore.catalog.author.AuthorMapper;
import com.nguyenduyhung.bookstore.catalog.book.dto.BookResponse;
import com.nguyenduyhung.bookstore.catalog.book.dto.BookSummaryResponse;
import com.nguyenduyhung.bookstore.catalog.category.CategoryMapper;
import com.nguyenduyhung.bookstore.review.RatingSummary;

import java.util.Comparator;
import java.util.List;

/**
 * Lưu ý: mapper đọc book.getCategory() và book.getAuthors(), là quan hệ LAZY.
 * Vì vậy PHẢI gọi mapper bên trong transaction (trong Service); gọi ở Controller
 * sẽ gặp LazyInitializationException do đã tắt open-in-view.
 */
public final class BookMapper {

    // Set không có thứ tự; sắp theo tên để JSON trả ra ổn định giữa các lần gọi.
    private static final Comparator<Author> BY_NAME = Comparator.comparing(Author::getName);

    private BookMapper() {
    }

    public static BookResponse toResponse(Book book, RatingSummary rating) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getIsbn(),
                book.getDescription(),
                book.getPrice(),
                book.getStockQuantity(),
                book.getPublishedYear(),
                CategoryMapper.toResponse(book.getCategory()),
                book.getAuthors().stream().sorted(BY_NAME).map(AuthorMapper::toSummary).toList(),
                rating.roundedAverage(),
                rating.count(),
                book.getCreatedAt(),
                book.getUpdatedAt());
    }

    public static BookSummaryResponse toSummary(Book book) {
        List<String> authorNames = book.getAuthors().stream().sorted(BY_NAME).map(Author::getName).toList();
        return new BookSummaryResponse(
                book.getId(),
                book.getTitle(),
                book.getPrice(),
                book.getStockQuantity() > 0,
                book.getCategory().getName(),
                authorNames);
    }
}
