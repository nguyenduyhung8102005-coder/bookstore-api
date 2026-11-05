package com.nguyenduyhung.bookstore.catalog.author;

import com.nguyenduyhung.bookstore.catalog.author.dto.AuthorResponse;
import com.nguyenduyhung.bookstore.catalog.author.dto.AuthorSummaryResponse;

public final class AuthorMapper {

    private AuthorMapper() {
    }

    public static AuthorResponse toResponse(Author author) {
        return new AuthorResponse(author.getId(), author.getName(), author.getBio());
    }

    public static AuthorSummaryResponse toSummary(Author author) {
        return new AuthorSummaryResponse(author.getId(), author.getName());
    }
}
