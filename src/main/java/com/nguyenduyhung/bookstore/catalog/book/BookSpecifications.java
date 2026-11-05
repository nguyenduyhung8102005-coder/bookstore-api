package com.nguyenduyhung.bookstore.catalog.book;

import com.nguyenduyhung.bookstore.catalog.author.Author;
import com.nguyenduyhung.bookstore.catalog.book.dto.BookSearchCriteria;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Xây câu truy vấn ĐỘNG cho việc lọc sách bằng JPA Specification.
 *
 * Vấn đề: có 5 tiêu chí lọc, mỗi tiêu chí có thể có hoặc không. Viết method kiểu
 * findByTitleAndCategoryAndPriceBetween... cho mọi tổ hợp là không thể (2^5 = 32).
 * Specification cho phép mỗi tiêu chí là một "mảnh điều kiện" riêng, rồi ghép lại
 * bằng and() chỉ với những tiêu chí client thực sự gửi lên.
 *
 * Tên thuộc tính viết bằng chuỗi ("title", "price"). Phương án an toàn hơn là
 * JPA Static Metamodel (Book_.title, sinh tự động lúc compile): gõ sai sẽ báo
 * lỗi compile thay vì lỗi lúc chạy.
 */
public final class BookSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private BookSpecifications() {
    }

    public static Specification<Book> from(BookSearchCriteria criteria) {
        Specification<Book> spec = isActive();
        if (StringUtils.hasText(criteria.keyword())) {
            spec = spec.and(titleContains(criteria.keyword()));
        }
        if (criteria.categoryId() != null) {
            spec = spec.and(hasCategory(criteria.categoryId()));
        }
        if (criteria.authorId() != null) {
            spec = spec.and(hasAuthor(criteria.authorId()));
        }
        if (criteria.minPrice() != null) {
            spec = spec.and(priceAtLeast(criteria.minPrice()));
        }
        if (criteria.maxPrice() != null) {
            spec = spec.and(priceAtMost(criteria.maxPrice()));
        }
        return spec;
    }

    static Specification<Book> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    /**
     * Tìm không phân biệt hoa thường: lower(title) LIKE '%từ khóa%'.
     * Từ khóa được escape: nếu không, người dùng gõ "%" hoặc "_" (ký tự đại diện
     * của LIKE) sẽ khớp với mọi sách thay vì tìm đúng ký tự đó.
     * Đây KHÔNG phải SQL injection: giá trị vẫn được truyền dưới dạng tham số.
     */
    static Specification<Book> titleContains(String keyword) {
        String pattern = "%" + escapeLike(keyword.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE);
    }

    static Specification<Book> hasCategory(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    /**
     * JOIN sang bảng nối book_authors. Join một collection thường làm một sách
     * xuất hiện nhiều lần (mỗi tác giả một dòng), nhưng ở đây ta lọc theo ĐÚNG
     * MỘT authorId, và cặp (book_id, author_id) là khóa chính, nên mỗi sách khớp
     * tối đa một dòng: không cần DISTINCT.
     */
    static Specification<Book> hasAuthor(Long authorId) {
        return (root, query, cb) -> {
            Join<Book, Author> authors = root.join("authors");
            return cb.equal(authors.get("id"), authorId);
        };
    }

    static Specification<Book> priceAtLeast(BigDecimal minPrice) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    static Specification<Book> priceAtMost(BigDecimal maxPrice) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    /** "50%_off" -> "50\%\_off". Escape chính ký tự escape trước tiên. */
    static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
