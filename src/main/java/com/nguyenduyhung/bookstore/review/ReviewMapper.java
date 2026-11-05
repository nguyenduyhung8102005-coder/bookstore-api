package com.nguyenduyhung.bookstore.review;

import com.nguyenduyhung.bookstore.review.dto.ReviewResponse;

public final class ReviewMapper {

    private ReviewMapper() {
    }

    public static ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getRating(),
                review.getComment(),
                review.getUser().getFullName(),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }
}
