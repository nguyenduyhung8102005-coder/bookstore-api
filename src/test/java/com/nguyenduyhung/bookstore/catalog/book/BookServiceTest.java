package com.nguyenduyhung.bookstore.catalog.book;

import com.nguyenduyhung.bookstore.catalog.author.Author;
import com.nguyenduyhung.bookstore.catalog.author.AuthorService;
import com.nguyenduyhung.bookstore.catalog.author.dto.AuthorSummaryResponse;
import com.nguyenduyhung.bookstore.catalog.book.dto.BookRequest;
import com.nguyenduyhung.bookstore.catalog.book.dto.BookResponse;
import com.nguyenduyhung.bookstore.catalog.book.dto.BookSearchCriteria;
import com.nguyenduyhung.bookstore.catalog.category.Category;
import com.nguyenduyhung.bookstore.catalog.category.CategoryService;
import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import com.nguyenduyhung.bookstore.review.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    private static final String ISBN = "9786041234567";

    @Mock
    private BookRepository bookRepository;
    @Mock
    private CategoryService categoryService;
    @Mock
    private AuthorService authorService;
    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    void create_success_mapsCategoryAndAuthorsSortedByName() {
        when(bookRepository.existsByIsbn(ISBN)).thenReturn(false);
        when(categoryService.getEntity(1L)).thenReturn(Category.create("Văn học"));
        when(authorService.getEntities(Set.of(2L, 3L)))
                .thenReturn(Set.of(Author.create("Tô Hoài", null), Author.create("Nam Cao", null)));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse response = bookService.create(request(ISBN, Set.of(2L, 3L)));

        assertThat(response.title()).isEqualTo("Dế Mèn phiêu lưu ký");
        assertThat(response.category().slug()).isEqualTo("van-hoc");
        assertThat(response.authors()).extracting(AuthorSummaryResponse::name).containsExactly("Nam Cao", "Tô Hoài");
    }

    @Test
    void create_duplicateIsbn_isRejectedBeforeLoadingAnythingElse() {
        when(bookRepository.existsByIsbn(ISBN)).thenReturn(true);

        assertThatThrownBy(() -> bookService.create(request(ISBN, Set.of(2L))))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ISBN_ALREADY_EXISTS);
        verifyNoInteractions(categoryService, authorService);
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void update_isbnUsedByAnotherBook_isRejected() {
        when(bookRepository.findById(5L)).thenReturn(Optional.of(existingBook()));
        when(bookRepository.existsByIsbnAndIdNot(ISBN, 5L)).thenReturn(true);

        assertThatThrownBy(() -> bookService.update(5L, request(ISBN, Set.of(2L))))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ISBN_ALREADY_EXISTS);
    }

    @Test
    void delete_isSoftDelete() {
        Book book = existingBook();
        when(bookRepository.findById(5L)).thenReturn(Optional.of(book));

        bookService.delete(5L);

        assertThat(book.isActive()).isFalse();
        verify(bookRepository, never()).delete(any(Book.class));
    }

    @Test
    void getActiveById_softDeletedBook_returnsNotFound() {
        when(bookRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getActiveById(5L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    // ===================== search =====================

    @Test
    void search_appendsIdAsFinalSortKey_forStablePagination() {
        when(bookRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        bookService.search(NO_FILTER, PageRequest.of(0, 20, Sort.by("price")));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(bookRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getSort()).containsExactly(Sort.Order.asc("price"), Sort.Order.asc("id"));
    }

    @Test
    void search_sortByPropertyOutsideWhitelist_isRejected() {
        assertThatThrownBy(() -> bookService.search(NO_FILTER, PageRequest.of(0, 20, Sort.by("stockQuantity"))))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("stockQuantity")
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_PARAMETER);
        verifyNoInteractions(bookRepository);
    }

    @Test
    void search_minPriceGreaterThanMaxPrice_isRejected() {
        var criteria = new BookSearchCriteria(null, null, null, new BigDecimal("200000"), new BigDecimal("100000"));

        assertThatThrownBy(() -> bookService.search(criteria, PageRequest.of(0, 20)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    private static final BookSearchCriteria NO_FILTER = new BookSearchCriteria(null, null, null, null, null);

    private static BookRequest request(String isbn, Set<Long> authorIds) {
        return new BookRequest("Dế Mèn phiêu lưu ký", isbn, "Truyện thiếu nhi kinh điển",
                new BigDecimal("75000.00"), 10, 1941, 1L, authorIds);
    }

    private static Book existingBook() {
        return Book.create("Sách cũ", "9780000000001", null, new BigDecimal("50000.00"), 5, 2000,
                Category.create("Văn học"), Set.of(Author.create("Nam Cao", null)));
    }
}
