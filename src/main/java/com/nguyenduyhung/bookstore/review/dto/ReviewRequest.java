package com.nguyenduyhung.bookstore.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Dùng cho cả viết mới (POST) và sửa (PUT) đánh giá. Nhận xét không bắt buộc. */
public record ReviewRequest(
        @NotNull @Min(1) @Max(5)
        Integer rating,

        @Size(max = 2000)
        String comment
) {
}
