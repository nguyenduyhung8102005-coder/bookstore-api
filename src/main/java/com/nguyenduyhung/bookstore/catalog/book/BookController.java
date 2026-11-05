package com.nguyenduyhung.bookstore.catalog.book;

import com.nguyenduyhung.bookstore.catalog.book.dto.BookResponse;
import com.nguyenduyhung.bookstore.catalog.book.dto.BookSearchCriteria;
import com.nguyenduyhung.bookstore.catalog.book.dto.BookSummaryResponse;
import com.nguyenduyhung.bookstore.common.response.ApiResponse;
import com.nguyenduyhung.bookstore.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Books")
@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    /**
     * Ví dụ: GET /api/v1/books?keyword=dế mèn&categoryId=1&minPrice=50000&sort=price,asc&page=0&size=20
     * Sắp xếp được theo: createdAt (mặc định, mới nhất trước), price, title.
     */
    @Operation(summary = "Tìm kiếm, lọc, sắp xếp và phân trang sách đang bán")
    @GetMapping
    public ApiResponse<PageResponse<BookSummaryResponse>> search(
            @ParameterObject @Valid BookSearchCriteria criteria,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(bookService.search(criteria, pageable));
    }

    @Operation(summary = "Chi tiết sách")
    @GetMapping("/{id}")
    public ApiResponse<BookResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(bookService.getActiveById(id));
    }
}
