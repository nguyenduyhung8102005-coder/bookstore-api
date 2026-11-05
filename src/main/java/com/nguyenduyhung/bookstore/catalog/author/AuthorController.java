package com.nguyenduyhung.bookstore.catalog.author;

import com.nguyenduyhung.bookstore.catalog.author.dto.AuthorResponse;
import com.nguyenduyhung.bookstore.common.response.ApiResponse;
import com.nguyenduyhung.bookstore.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authors")
@RestController
@RequestMapping("/api/v1/authors")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService authorService;

    /**
     * Spring tự đọc ?page=0&size=20&sort=name,asc thành đối tượng Pageable.
     * @PageableDefault: giá trị dùng khi client không truyền.
     * @ParameterObject: để Swagger hiển thị page/size/sort thành 3 ô riêng.
     */
    @Operation(summary = "Danh sách tác giả (phân trang)")
    @GetMapping
    public ApiResponse<PageResponse<AuthorResponse>> findAll(
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ApiResponse.success(authorService.findAll(pageable));
    }

    @Operation(summary = "Chi tiết tác giả")
    @GetMapping("/{id}")
    public ApiResponse<AuthorResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(authorService.getById(id));
    }
}
