package com.nguyenduyhung.bookstore.catalog.author.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthorRequest(
        @NotBlank @Size(max = 150)
        String name,

        @Size(max = 5000)
        String bio
) {
}
