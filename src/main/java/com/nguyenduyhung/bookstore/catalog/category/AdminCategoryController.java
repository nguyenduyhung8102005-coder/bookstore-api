package com.nguyenduyhung.bookstore.catalog.category;

import com.nguyenduyhung.bookstore.catalog.category.dto.CategoryRequest;
import com.nguyenduyhung.bookstore.catalog.category.dto.CategoryResponse;
import com.nguyenduyhung.bookstore.common.config.OpenApiConfig;
import com.nguyenduyhung.bookstore.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tách controller quản trị riêng: mọi đường dẫn nằm dưới /api/v1/admin/** nên
 * một luật trong SecurityConfig đã chặn được cả nhóm.
 *
 * @PreAuthorize ở đây là lớp bảo vệ THỨ HAI (defense in depth): nếu sau này ai
 * đó lỡ sửa luật URL trong SecurityConfig, controller vẫn không mở cho USER.
 */
@Tag(name = "Admin - Categories")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Tạo thể loại")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success(categoryService.create(request), "Category created");
    }

    @Operation(summary = "Đổi tên thể loại")
    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success(categoryService.update(id, request), "Category updated");
    }

    @Operation(summary = "Xóa thể loại (chỉ khi không còn sách nào thuộc thể loại)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.success(null, "Category deleted");
    }
}
