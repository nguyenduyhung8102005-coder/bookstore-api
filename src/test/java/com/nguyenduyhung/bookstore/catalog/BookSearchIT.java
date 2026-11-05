package com.nguyenduyhung.bookstore.catalog;

import com.nguyenduyhung.bookstore.AbstractIntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm tra tìm kiếm trên PostgreSQL thật.
 *
 * Mỗi test tạo một THỂ LOẠI RIÊNG với 5 cuốn sách, và mọi truy vấn đều lọc theo
 * categoryId đó. Nhờ vậy kết quả không bị lẫn sách do các test khác tạo ra trong
 * cùng database.
 *
 * Dữ liệu mẫu (giá):
 *   Dế Mèn phiêu lưu ký      50.000   tác giả A
 *   Tắt đèn                  60.000   tác giả B
 *   Giảm giá 50% sách hay    80.000   tác giả A, B
 *   Số đỏ                    95.000   tác giả A
 *   Đất rừng phương Nam     120.000   tác giả B
 */
class BookSearchIT extends AbstractIntegrationTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private String adminToken;
    private Long categoryId;
    private Long authorA;
    private Long authorB;

    @BeforeEach
    void createSampleBooks() throws Exception {
        adminToken = adminAccessToken();
        categoryId = postAndGetId("/api/v1/admin/categories", Map.of("name", "Search " + UUID.randomUUID()));
        authorA = postAndGetId("/api/v1/admin/authors", Map.of("name", "Tác giả A"));
        authorB = postAndGetId("/api/v1/admin/authors", Map.of("name", "Tác giả B"));

        createBook("Dế Mèn phiêu lưu ký", "50000", List.of(authorA));
        createBook("Tắt đèn", "60000", List.of(authorB));
        createBook("Giảm giá 50% sách hay", "80000", List.of(authorA, authorB));
        createBook("Số đỏ", "95000", List.of(authorA));
        createBook("Đất rừng phương Nam", "120000", List.of(authorB));
    }

    // ===================== Lọc =====================

    @Test
    void filterByCategory_returnsOnlyBooksOfThatCategory() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("categoryId", categoryId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(5));
    }

    @Test
    void keyword_isCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/v1/books")
                        .param("categoryId", categoryId.toString())
                        .param("keyword", "MÈN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].title").value(contains("Dế Mèn phiêu lưu ký")));
    }

    /**
     * "%" trong LIKE nghĩa là "bất kỳ chuỗi nào". Nếu không escape, từ khóa "%"
     * sẽ khớp cả 5 cuốn. Đã escape thì chỉ khớp sách có ký tự "%" trong tên.
     */
    @Test
    void keyword_wildcardCharacterIsTreatedLiterally() throws Exception {
        mockMvc.perform(get("/api/v1/books")
                        .param("categoryId", categoryId.toString())
                        .param("keyword", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].title").value(contains("Giảm giá 50% sách hay")));
    }

    @Test
    void filterByAuthor_includesBooksWithSeveralAuthors() throws Exception {
        mockMvc.perform(get("/api/v1/books")
                        .param("categoryId", categoryId.toString())
                        .param("authorId", authorA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].title").value(containsInAnyOrder(
                        "Dế Mèn phiêu lưu ký", "Giảm giá 50% sách hay", "Số đỏ")));
    }

    @Test
    void filterByPriceRange_isInclusive() throws Exception {
        mockMvc.perform(get("/api/v1/books")
                        .param("categoryId", categoryId.toString())
                        .param("minPrice", "60000")
                        .param("maxPrice", "95000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].title").value(containsInAnyOrder(
                        "Tắt đèn", "Giảm giá 50% sách hay", "Số đỏ")));
    }

    // ===================== Sắp xếp & phân trang =====================

    @Test
    void sortByPriceAscending_withPagination() throws Exception {
        mockMvc.perform(get("/api/v1/books")
                        .param("categoryId", categoryId.toString())
                        .param("sort", "price,asc")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                // Trang thứ 2 (page=1), mỗi trang 2 cuốn -> cuốn thứ 3 và 4 theo giá tăng dần
                .andExpect(jsonPath("$.data.content[*].title").value(contains("Giảm giá 50% sách hay", "Số đỏ")))
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(3));
    }

    @Test
    void sortByPropertyOutsideWhitelist_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("sort", "stockQuantity,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    // ===================== Tham số không hợp lệ =====================

    @Test
    void minPriceGreaterThanMaxPrice_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("minPrice", "200000").param("maxPrice", "100000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void nonNumericPrice_returnsFieldError() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("minPrice", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("minPrice"))
                .andExpect(jsonPath("$.errors[0].message").value("Invalid value 'abc'"));
    }

    // ===================== N+1 =====================

    /**
     * Đếm số câu SQL thực sự chạy khi lấy một trang 5 cuốn sách.
     *
     * Nếu còn N+1: 1 câu lấy trang + 1 câu lấy thể loại + 5 câu lấy tác giả
     * (mỗi sách một câu) = 7 câu, và tăng tuyến tính theo số sách trong trang.
     * Đã sửa: 1 câu lấy trang (JOIN sẵn thể loại) + 1 câu lấy tác giả của cả trang
     * (+ 1 câu đếm tổng nếu kết quả nhiều hơn một trang) = tối đa 3 câu.
     */
    @Test
    void listingBooks_runsConstantNumberOfQueries() throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        // Nếu quên bật generate_statistics, mọi bộ đếm luôn là 0 và test sẽ pass "giả".
        assertThat(statistics.isStatisticsEnabled()).as("hibernate.generate_statistics").isTrue();
        statistics.clear();

        mockMvc.perform(get("/api/v1/books").param("categoryId", categoryId.toString()).param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].authorNames").isNotEmpty());

        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    // ===================== Helper =====================

    private Long postAndGetId(String url, Object body) throws Exception {
        return dataId(mockMvc.perform(post(url)
                        .header(HttpHeaders.AUTHORIZATION, authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(body)))
                .andExpect(status().isCreated()));
    }

    private void createBook(String title, String price, List<Long> authorIds) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("isbn", "978" + ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
        body.put("price", new BigDecimal(price));
        body.put("stockQuantity", 10);
        body.put("categoryId", categoryId);
        body.put("authorIds", authorIds);
        postAndGetId("/api/v1/admin/books", body);
    }
}
