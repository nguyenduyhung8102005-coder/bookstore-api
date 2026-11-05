package com.nguyenduyhung.bookstore.review.dto;

import java.time.Instant;

/** Chỉ lộ TÊN người viết, không lộ email hay id của họ. */
public record ReviewResponse(
        Long id,
        int rating,
        String comment,
        String reviewerName,
        Instant createdAt,
        Instant updatedAt
) {
}
