package com.nguyenduyhung.bookstore.catalog.category;

import com.nguyenduyhung.bookstore.catalog.book.BookRepository;
import com.nguyenduyhung.bookstore.catalog.category.dto.CategoryRequest;
import com.nguyenduyhung.bookstore.catalog.category.dto.CategoryResponse;
import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @InjectMocks: Mockito tự tạo CategoryService và truyền các @Mock vào constructor.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void create_generatesSlugFromName() {
        when(categoryRepository.existsBySlug("van-hoc")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = categoryService.create(new CategoryRequest("  Văn học "));

        assertThat(response.name()).isEqualTo("Văn học");
        assertThat(response.slug()).isEqualTo("van-hoc");
    }

    @Test
    void create_nameDifferingOnlyInCaseOrAccents_isRejected() {
        when(categoryRepository.existsBySlug("van-hoc")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(new CategoryRequest("VĂN HỌC")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CATEGORY_ALREADY_EXISTS);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_nameWithoutLettersOrDigits_isRejected() {
        assertThatThrownBy(() -> categoryService.create(new CategoryRequest("!!!")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void delete_categoryThatStillHasBooks_isRejected() {
        Category category = Category.create("Văn học");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(bookRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CATEGORY_IN_USE);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void delete_unknownCategory_returnsNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(99L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
