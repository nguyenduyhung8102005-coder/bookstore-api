package com.nguyenduyhung.bookstore.catalog.book;

import com.nguyenduyhung.bookstore.catalog.author.Author;
import com.nguyenduyhung.bookstore.catalog.category.Category;
import com.nguyenduyhung.bookstore.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@Entity
@Table(name = "books")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "published_year")
    private Integer publishedYear;

    @Column(nullable = false)
    private boolean active = true;

    /*
     * Optimistic locking: mỗi câu UPDATE có thêm "WHERE version = <giá trị đã đọc>"
     * và tăng version lên 1. Nếu ai đó đã sửa dòng này trong lúc ta đang xử lý,
     * điều kiện không khớp, UPDATE ảnh hưởng 0 dòng và Hibernate ném
     * OptimisticLockException. GĐ 5 dựa vào cơ chế này để chống bán quá số lượng.
     */
    @Version
    private Long version;

    // Mặc định của @ManyToOne là EAGER, nên luôn khai báo LAZY một cách tường minh.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /*
     * @ManyToMany mặc định đã là LAZY. Dùng Set thay vì List: với List, mỗi lần
     * sửa danh sách Hibernate xóa TOÀN BỘ dòng trong bảng nối rồi chèn lại; với
     * Set, nó chỉ thêm/xóa đúng những dòng thay đổi.
     * Quan hệ một chiều: Author không có danh sách books, vì ta không cần đi từ
     * tác giả sang sách qua object (khi cần sẽ truy vấn qua BookRepository).
     */
    @ManyToMany
    @JoinTable(
            name = "book_authors",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id"))
    private Set<Author> authors = new HashSet<>();

    public static Book create(String title, String isbn, String description, BigDecimal price,
                              int stockQuantity, Integer publishedYear,
                              Category category, Set<Author> authors) {
        Book book = new Book();
        book.update(title, isbn, description, price, stockQuantity, publishedYear, category, authors);
        return book;
    }

    /** PUT thay thế toàn bộ thông tin sách, nên mọi field đều được gán lại. */
    public void update(String title, String isbn, String description, BigDecimal price,
                       int stockQuantity, Integer publishedYear,
                       Category category, Set<Author> authors) {
        this.title = title.trim();
        this.isbn = isbn;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.publishedYear = publishedYear;
        this.category = category;
        // Sửa trên chính collection Hibernate đang theo dõi, không gán Set mới,
        // để Hibernate tính được chính xác dòng nào cần thêm/xóa.
        this.authors.clear();
        this.authors.addAll(authors);
    }

    /** Soft delete. Gọi nhiều lần vẫn cho cùng kết quả (idempotent). */
    public void deactivate() {
        this.active = false;
    }
}
