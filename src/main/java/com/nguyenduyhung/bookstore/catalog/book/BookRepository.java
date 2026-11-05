package com.nguyenduyhung.bookstore.catalog.book;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * JpaSpecificationExecutor bổ sung các method nhận Specification (findAll(spec, pageable)...).
 */
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    /**
     * Chi tiết một cuốn sách: lấy luôn category và authors bằng JOIN trong cùng
     * một câu SQL. Fetch cả collection ở đây là an toàn vì chỉ có MỘT sách,
     * không có phân trang.
     */
    @EntityGraph(attributePaths = {"category", "authors"})
    Optional<Book> findByIdAndActiveTrue(Long id);

    /**
     * Danh sách có lọc + phân trang. Cách chống N+1 gồm HAI phần:
     *
     * 1. category (quan hệ *-to-ONE): JOIN thẳng vào câu chính bằng @EntityGraph.
     *    Mỗi sách chỉ có một thể loại nên số dòng không đổi, LIMIT/OFFSET vẫn đúng.
     *
     * 2. authors (quan hệ *-to-MANY): KHÔNG join vào câu chính. Join collection làm
     *    mỗi sách thành nhiều dòng; khi đó LIMIT 20 không còn nghĩa là "20 sách",
     *    nên Hibernate phải tải TOÀN BỘ kết quả về RAM rồi tự cắt trang
     *    (cảnh báo HHH90003004), rất nguy hiểm khi bảng lớn.
     *    Thay vào đó, hibernate.default_batch_fetch_size (application.yml) gom việc
     *    tải authors của cả trang thành MỘT câu "... WHERE book_id IN (...)".
     *
     * Kết quả: tối đa 3 câu SQL (trang dữ liệu + đếm tổng + authors), bất kể size.
     */
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Book> findAll(Specification<Book> spec, Pageable pageable);

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);

    /** Đếm cả sách đã soft delete: dòng vẫn còn trong DB và vẫn trỏ tới thể loại. */
    boolean existsByCategoryId(Long categoryId);

    /** "AuthorsId" = đi qua collection authors rồi so sánh id (Spring Data tự join bảng nối). */
    boolean existsByAuthorsId(Long authorId);
}
