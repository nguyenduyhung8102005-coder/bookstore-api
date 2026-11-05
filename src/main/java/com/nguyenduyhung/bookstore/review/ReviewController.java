package com.nguyenduyhung.bookstore.review;

import com.nguyenduyhung.bookstore.common.config.OpenApiConfig;
import com.nguyenduyhung.bookstore.common.response.ApiResponse;
import com.nguyenduyhung.bookstore.common.response.PageResponse;
import com.nguyenduyhung.bookstore.review.dto.ReviewRequest;
import com.nguyenduyhung.bookstore.review.dto.ReviewResponse;
import com.nguyenduyhung.bookstore.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hai kiểu đường dẫn:
 * - /books/{bookId}/reviews: đánh giá là tài nguyên CON của sách (xem, viết mới).
 * - /reviews/{id}: thao tác trên MỘT đánh giá đã có (sửa, xóa); id đã đủ xác định nó.
 *
 * Quyền: GET /api/v1/books/** là public (SecurityConfig), POST cần đăng nhập.
 */
@Tag(name = "Reviews")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Danh sách đánh giá của một cuốn sách (mới nhất trước)")
    @GetMapping("/api/v1/books/{bookId}/reviews")
    public ApiResponse<PageResponse<ReviewResponse>> getReviewsOfBook(
            @PathVariable Long bookId,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(reviewService.getReviewsOfBook(bookId, pageable));
    }

    @Operation(summary = "Viết đánh giá (chỉ khi đã có đơn hoàn thành chứa cuốn sách này)")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PostMapping("/api/v1/books/{bookId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> create(@AuthenticationPrincipal AuthUser currentUser,
                                              @PathVariable Long bookId,
                                              @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.success(reviewService.create(currentUser.id(), bookId, request), "Review created");
    }

    @Operation(summary = "Sửa đánh giá của chính mình")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PutMapping("/api/v1/reviews/{id}")
    public ApiResponse<ReviewResponse> update(@AuthenticationPrincipal AuthUser currentUser,
                                              @PathVariable Long id,
                                              @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.success(reviewService.update(currentUser, id, request), "Review updated");
    }

    @Operation(summary = "Xóa đánh giá (người viết hoặc ADMIN)")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @DeleteMapping("/api/v1/reviews/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser currentUser, @PathVariable Long id) {
        reviewService.delete(currentUser, id);
        return ApiResponse.success(null, "Review deleted");
    }
}
