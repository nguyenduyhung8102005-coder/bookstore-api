package com.nguyenduyhung.bookstore.catalog.book;

import com.nguyenduyhung.bookstore.catalog.author.Author;
import com.nguyenduyhung.bookstore.catalog.author.AuthorService;
import com.nguyenduyhung.bookstore.catalog.book.dto.BookRequest;
import com.nguyenduyhung.bookstore.catalog.book.dto.BookResponse;
import com.nguyenduyhung.bookstore.catalog.book.dto.BookSummaryResponse;
import com.nguyenduyhung.bookstore.catalog.category.Category;
import com.nguyenduyhung.bookstore.catalog.category.CategoryService;
import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import com.nguyenduyhung.bookstore.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryService categoryService;
    private final AuthorService authorService;

    // ===================== Public =====================

    /** Sách đã soft delete coi như không tồn tại với người dùng: trả 404. */
    public BookResponse getActiveById(Long id) {
        return bookRepository.findByIdAndActiveTrue(id)
                .map(BookMapper::toResponse)
                .orElseThrow(() -> AppException.notFound("Book", id));
    }

    /** Có lỗi N+1 (chủ đích, xem BookRepository.findByActiveTrue). GĐ 3 sẽ sửa. */
    public PageResponse<BookSummaryResponse> findActive(Pageable pageable) {
        return PageResponse.from(bookRepository.findByActiveTrue(pageable).map(BookMapper::toSummary));
    }

    // ===================== Admin =====================

    @Transactional
    public BookResponse create(BookRequest request) {
        if (bookRepository.existsByIsbn(request.isbn())) {
            throw new AppException(ErrorCode.ISBN_ALREADY_EXISTS);
        }
        Category category = categoryService.getEntity(request.categoryId());
        Set<Author> authors = authorService.getEntities(request.authorIds());

        Book book = Book.create(request.title(), request.isbn(), request.description(), request.price(),
                request.stockQuantity(), request.publishedYear(), category, authors);
        return BookMapper.toResponse(bookRepository.save(book));
    }

    /**
     * Cho phép sửa cả sách đã soft delete (admin vẫn cần chỉnh dữ liệu), nên dùng
     * findById thay vì findByIdAndActiveTrue.
     */
    @Transactional
    public BookResponse update(Long id, BookRequest request) {
        Book book = getEntity(id);
        if (bookRepository.existsByIsbnAndIdNot(request.isbn(), id)) {
            throw new AppException(ErrorCode.ISBN_ALREADY_EXISTS);
        }
        Category category = categoryService.getEntity(request.categoryId());
        Set<Author> authors = authorService.getEntities(request.authorIds());

        book.update(request.title(), request.isbn(), request.description(), request.price(),
                request.stockQuantity(), request.publishedYear(), category, authors);

        // Bình thường UPDATE chỉ chạy lúc commit. Ở đây flush ngay để JPA Auditing
        // cập nhật updatedAt và @Version tăng TRƯỚC khi map sang response; nếu
        // không, client nhận về updatedAt cũ.
        bookRepository.flush();
        return BookMapper.toResponse(book);
    }

    /**
     * Soft delete: chỉ đặt active = false. Dòng dữ liệu được giữ lại vì đơn hàng
     * cũ (GĐ 5) còn tham chiếu tới sách này.
     */
    @Transactional
    public void delete(Long id) {
        getEntity(id).deactivate();
    }

    private Book getEntity(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Book", id));
    }
}
