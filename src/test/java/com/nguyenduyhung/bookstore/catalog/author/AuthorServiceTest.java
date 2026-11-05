package com.nguyenduyhung.bookstore.catalog.author;

import com.nguyenduyhung.bookstore.catalog.book.BookRepository;
import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private AuthorService authorService;

    @Test
    void getEntities_allFound_returnsThemAll() {
        Author a = author(1L, "Nguyễn Nhật Ánh");
        Author b = author(2L, "Tô Hoài");
        when(authorRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(a, b));

        assertThat(authorService.getEntities(Set.of(1L, 2L))).containsExactlyInAnyOrder(a, b);
    }

    @Test
    void getEntities_someMissing_reportsExactlyTheMissingIds() {
        when(authorRepository.findAllById(Set.of(1L, 2L, 3L))).thenReturn(List.of(author(1L, "Tô Hoài")));

        assertThatThrownBy(() -> authorService.getEntities(Set.of(1L, 2L, 3L)))
                .isInstanceOf(AppException.class)
                .hasMessage("Author not found with id: [2, 3]")
                .extracting("errorCode").isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void delete_authorStillLinkedToBooks_isRejected() {
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author(1L, "Tô Hoài")));
        when(bookRepository.existsByAuthorsId(1L)).thenReturn(true);

        assertThatThrownBy(() -> authorService.delete(1L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTHOR_IN_USE);
        verify(authorRepository, never()).delete(any());
    }

    private static Author author(Long id, String name) {
        Author author = Author.create(name, null);
        ReflectionTestUtils.setField(author, "id", id);
        return author;
    }
}
