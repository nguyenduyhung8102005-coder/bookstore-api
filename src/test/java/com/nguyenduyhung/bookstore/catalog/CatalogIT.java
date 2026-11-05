package com.nguyenduyhung.bookstore.catalog;

import com.nguyenduyhung.bookstore.AbstractIntegrationTest;
import com.nguyenduyhung.bookstore.catalog.book.Book;
import com.nguyenduyhung.bookstore.catalog.book.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogIT extends AbstractIntegrationTest {

    @Autowired
    private BookRepository bookRepository;

    private String adminToken;

    @BeforeEach
    void loginAsAdmin() throws Exception {
        adminToken = adminAccessToken();
    }

    // ===================== Luồng chính =====================

    @Test
    void adminCreatesBook_andAnyoneCanReadIt() throws Exception {
        Long categoryId = createCategory("Thiếu nhi " + suffix());
        Long authorId = createAuthor("Tô Hoài");
        String isbn = randomIsbn();

        Long bookId = dataId(createBook(isbn, categoryId, List.of(authorId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isbn").value(isbn))
                .andExpect(jsonPath("$.data.price").value(75000.0))
                .andExpect(jsonPath("$.data.category.id").value(categoryId))
                .andExpect(jsonPath("$.data.authors[0].name").value("Tô Hoài")));

        // Không cần token: đọc sách là API public
        mockMvc.perform(get("/api/v1/books/{id}", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Dế Mèn phiêu lưu ký"));

        mockMvc.perform(get("/api/v1/books").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].id").value(hasItem(bookId.intValue())));
    }

    @Test
    void adminUpdatesBook() throws Exception {
        Long categoryId = createCategory("Văn học " + suffix());
        Long authorId = createAuthor("Nam Cao");
        Long bookId = dataId(createBook(randomIsbn(), categoryId, List.of(authorId)));

        Map<String, Object> body = bookBody(randomIsbn(), categoryId, List.of(authorId));
        body.put("title", "Chí Phèo");
        body.put("price", new BigDecimal("199000.50"));

        mockMvc.perform(put("/api/v1/admin/books/{id}", bookId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Chí Phèo"))
                .andExpect(jsonPath("$.data.price").value(199000.5));
    }

    /**
     * Soft delete: sách biến mất với người dùng nhưng dòng dữ liệu vẫn còn,
     * nên thể loại của nó vẫn bị coi là "đang được dùng".
     */
    @Test
    void softDeletedBook_isHiddenFromPublic_butStillExistsInDatabase() throws Exception {
        Long categoryId = createCategory("Lịch sử " + suffix());
        Long bookId = dataId(createBook(randomIsbn(), categoryId, List.of(createAuthor("Trần Trọng Kim"))));

        mockMvc.perform(delete("/api/v1/admin/books/{id}", bookId).header(HttpHeaders.AUTHORIZATION, authHeader(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/books/{id}", bookId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        assertThat(bookRepository.findById(bookId)).get().extracting(Book::isActive).isEqualTo(false);

        mockMvc.perform(delete("/api/v1/admin/categories/{id}", categoryId).header(HttpHeaders.AUTHORIZATION, authHeader(adminToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_IN_USE"));
    }

    // ===================== Luật nghiệp vụ =====================

    @Test
    void categoryNamesDifferingOnlyInCaseOrAccents_areDuplicates() throws Exception {
        String suffix = suffix();
        createCategory("Văn Học " + suffix);

        performCreateCategory("van hoc " + suffix)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_ALREADY_EXISTS"));
    }

    @Test
    void duplicateIsbn_isRejected() throws Exception {
        Long categoryId = createCategory("Khoa học " + suffix());
        Long authorId = createAuthor("Stephen Hawking");
        String isbn = randomIsbn();
        createBook(isbn, categoryId, List.of(authorId)).andExpect(status().isCreated());

        createBook(isbn, categoryId, List.of(authorId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ISBN_ALREADY_EXISTS"));
    }

    @Test
    void bookWithUnknownAuthor_isRejected() throws Exception {
        Long categoryId = createCategory("Kinh tế " + suffix());

        createBook(randomIsbn(), categoryId, List.of(999_999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Author not found with id: [999999999]"));
    }

    @Test
    void invalidBookBody_returnsFieldErrors() throws Exception {
        Map<String, Object> body = bookBody("12-34", 1L, List.of());
        body.put("price", new BigDecimal("-1"));

        mockMvc.perform(post("/api/v1/admin/books")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItems("isbn", "price", "authorIds")));
    }

    @Test
    void authorLinkedToBook_cannotBeDeleted_butUnusedAuthorCan() throws Exception {
        Long usedAuthorId = createAuthor("Nguyễn Du");
        Long unusedAuthorId = createAuthor("Hồ Xuân Hương");
        createBook(randomIsbn(), createCategory("Thơ " + suffix()), List.of(usedAuthorId));

        mockMvc.perform(delete("/api/v1/admin/authors/{id}", usedAuthorId).header(HttpHeaders.AUTHORIZATION, authHeader(adminToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTHOR_IN_USE"));

        mockMvc.perform(delete("/api/v1/admin/authors/{id}", unusedAuthorId).header(HttpHeaders.AUTHORIZATION, authHeader(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void unknownSortProperty_returnsBadRequest_insteadOf500() throws Exception {
        mockMvc.perform(get("/api/v1/authors").param("sort", "notAField"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    // ===================== Phân quyền =====================

    @Test
    void publicReadEndpoints_needNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/categories")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/authors")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/books")).andExpect(status().isOk());
    }

    @Test
    void user_cannotManageCatalog() throws Exception {
        mockMvc.perform(post("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(newUserAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("name", "Hack " + suffix()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void anonymous_cannotManageCatalog() throws Exception {
        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("name", "Hack " + suffix()))))
                .andExpect(status().isUnauthorized());
    }

    // ===================== Helper =====================

    private ResultActions performCreateCategory(String name) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/categories")
                .header(HttpHeaders.AUTHORIZATION, authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("name", name))));
    }

    private Long createCategory(String name) throws Exception {
        return dataId(performCreateCategory(name).andExpect(status().isCreated()));
    }

    private Long createAuthor(String name) throws Exception {
        return dataId(mockMvc.perform(post("/api/v1/admin/authors")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("name", name, "bio", "Tiểu sử"))))
                .andExpect(status().isCreated()));
    }

    private ResultActions createBook(String isbn, Long categoryId, List<Long> authorIds) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/books")
                .header(HttpHeaders.AUTHORIZATION, authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(bookBody(isbn, categoryId, authorIds))));
    }

    /** LinkedHashMap (không phải Map.of) để test có thể sửa từng field. */
    private static Map<String, Object> bookBody(String isbn, Long categoryId, List<Long> authorIds) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "Dế Mèn phiêu lưu ký");
        body.put("isbn", isbn);
        body.put("description", "Truyện thiếu nhi kinh điển");
        body.put("price", new BigDecimal("75000.00"));
        body.put("stockQuantity", 10);
        body.put("publishedYear", 1941);
        body.put("categoryId", categoryId);
        body.put("authorIds", authorIds);
        return body;
    }

    /** ISBN-13 ngẫu nhiên để các test (dùng chung DB) không đụng nhau. */
    private static String randomIsbn() {
        return "978" + ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L);
    }

    /** Hậu tố ngẫu nhiên cho tên thể loại (tên thể loại phải duy nhất). */
    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
