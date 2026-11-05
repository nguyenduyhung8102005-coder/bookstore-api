package com.nguyenduyhung.bookstore.catalog.book;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIdAndActiveTrue(Long id);

    /**
     * Danh sách sách đang bán, có phân trang.
     * CHỦ ĐÍCH để nguyên ở GĐ 2: mỗi sách khi map sang DTO sẽ lazy-load category
     * và authors, sinh thêm truy vấn cho từng dòng (lỗi N+1). GĐ 3 sẽ bật log SQL
     * để đo con số đó rồi mới sửa.
     */
    Page<Book> findByActiveTrue(Pageable pageable);

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);

    /** Đếm cả sách đã soft delete: dòng vẫn còn trong DB và vẫn trỏ tới thể loại. */
    boolean existsByCategoryId(Long categoryId);

    /** "AuthorsId" = đi qua collection authors rồi so sánh id (Spring Data tự join bảng nối). */
    boolean existsByAuthorsId(Long authorId);
}
