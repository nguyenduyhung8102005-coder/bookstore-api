package com.nguyenduyhung.bookstore.catalog.author;

import com.nguyenduyhung.bookstore.catalog.author.dto.AuthorRequest;
import com.nguyenduyhung.bookstore.catalog.author.dto.AuthorResponse;
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

@Tag(name = "Admin - Authors")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/admin/authors")
@RequiredArgsConstructor
public class AdminAuthorController {

    private final AuthorService authorService;

    @Operation(summary = "Tạo tác giả")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthorResponse> create(@Valid @RequestBody AuthorRequest request) {
        return ApiResponse.success(authorService.create(request), "Author created");
    }

    @Operation(summary = "Cập nhật tác giả")
    @PutMapping("/{id}")
    public ApiResponse<AuthorResponse> update(@PathVariable Long id, @Valid @RequestBody AuthorRequest request) {
        return ApiResponse.success(authorService.update(id, request), "Author updated");
    }

    @Operation(summary = "Xóa tác giả (chỉ khi không còn gắn với sách nào)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        authorService.delete(id);
        return ApiResponse.success(null, "Author deleted");
    }
}
