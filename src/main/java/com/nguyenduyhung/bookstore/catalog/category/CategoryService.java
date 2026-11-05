package com.nguyenduyhung.bookstore.catalog.category;

import com.nguyenduyhung.bookstore.catalog.book.BookRepository;
import com.nguyenduyhung.bookstore.catalog.category.dto.CategoryRequest;
import com.nguyenduyhung.bookstore.catalog.category.dto.CategoryResponse;
import com.nguyenduyhung.bookstore.common.exception.AppException;
import com.nguyenduyhung.bookstore.common.exception.ErrorCode;
import com.nguyenduyhung.bookstore.common.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * readOnly = true ở mức class: mọi method mặc định chỉ đọc (Hibernate bỏ qua
 * bước so sánh thay đổi để flush, nhẹ hơn). Method nào ghi dữ liệu thì khai báo
 * lại @Transactional riêng, và annotation ở method sẽ ghi đè annotation ở class.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;

    /** Số thể loại ít nên trả cả danh sách, không phân trang. */
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll(Sort.by("name")).stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String slug = validSlug(request.name());
        if (categoryRepository.existsBySlug(slug)) {
            throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }
        return CategoryMapper.toResponse(categoryRepository.save(Category.create(request.name())));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getEntity(id);
        String slug = validSlug(request.name());
        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }
        // Không cần gọi save(): category đang được Hibernate quản lý (managed),
        // mọi thay đổi trên nó tự được ghi xuống DB khi transaction commit (dirty checking).
        category.rename(request.name());
        return CategoryMapper.toResponse(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = getEntity(id);
        if (bookRepository.existsByCategoryId(id)) {
            throw new AppException(ErrorCode.CATEGORY_IN_USE);
        }
        categoryRepository.delete(category);
    }

    /** Dùng chung cho BookService khi gán thể loại cho sách. */
    public Category getEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Category", id));
    }

    /** Tên như "!!!" cho slug rỗng, không dùng được trong URL. */
    private String validSlug(String name) {
        String slug = SlugUtils.toSlug(name);
        if (slug.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Category name must contain letters or digits");
        }
        return slug;
    }
}
