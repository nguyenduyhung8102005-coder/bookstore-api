package com.nguyenduyhung.bookstore.catalog.book;

import com.nguyenduyhung.bookstore.catalog.author.Author;
import com.nguyenduyhung.bookstore.catalog.author.AuthorService;
import com.nguyenduyhung.bookstore.catalog.book.dto.BookRequest;
import com.nguyenduyhung.bookstore.catalog.book.dto.BookResponse;
import com.nguyenduyhung.bookstore.catalog.book.dto.BookSearchCriteria;
import com.nguyenduyhung.bookstore.catalog.book.dto.BookSummaryResponse;
import com.nguyenduyhung.bookstore.catalog.category.Category;
import com.nguyenduyhung.bookstore.catalog.category.CategoryService;
import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import com.nguyenduyhung.bookstore.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    /** List.of giữ thứ tự, để thông báo lỗi liệt kê các trường theo thứ tự cố định. */
    static final List<String> ALLOWED_SORT_PROPERTIES = List.of("createdAt", "price", "title");

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

    /**
     * Tìm kiếm sách đang bán theo các tiêu chí tùy chọn, có phân trang và sắp xếp.
     * Cách chống N+1 xem ở BookRepository.findAll(Specification, Pageable).
     */
    public PageResponse<BookSummaryResponse> search(BookSearchCriteria criteria, Pageable pageable) {
        validatePriceRange(criteria);
        Pageable safePageable = withStableSort(validateSort(pageable));
        return PageResponse.from(bookRepository
                .findAll(BookSpecifications.from(criteria), safePageable)
                .map(BookMapper::toSummary));
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

    // ===================== Kiểm tra tham số tìm kiếm =====================

    private static void validatePriceRange(BookSearchCriteria criteria) {
        if (criteria.minPrice() != null && criteria.maxPrice() != null
                && criteria.minPrice().compareTo(criteria.maxPrice()) > 0) {
            throw new AppException(ErrorCode.INVALID_PARAMETER, "minPrice must not be greater than maxPrice");
        }
    }

    /**
     * Chỉ cho sắp xếp theo các trường nằm trong danh sách cho phép (whitelist).
     * Không có whitelist, client có thể sort theo bất kỳ thuộc tính nào của entity:
     * - lộ thông tin không định công khai (ví dụ sort theo stockQuantity để suy ra tồn kho);
     * - sort theo cột không có index, làm câu truy vấn chậm trên bảng lớn;
     * - sort theo collection (authors.name) sinh JOIN làm một sách lặp lại nhiều lần.
     */
    private static Pageable validateSort(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                throw new AppException(ErrorCode.INVALID_PARAMETER,
                        "Sorting by '%s' is not supported. Allowed: %s"
                                .formatted(order.getProperty(), ALLOWED_SORT_PROPERTIES));
            }
        }
        return pageable;
    }

    /**
     * Thêm "id" làm tiêu chí sắp xếp cuối cùng. Nếu nhiều sách cùng giá mà chỉ
     * sort theo giá, DB được phép trả chúng theo thứ tự bất kỳ, và thứ tự đó có
     * thể khác nhau giữa hai lần truy vấn: một cuốn có thể xuất hiện ở cả trang 1
     * lẫn trang 2, cuốn khác thì không xuất hiện ở trang nào.
     */
    private static Pageable withStableSort(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().and(Sort.by("id")));
    }
}
