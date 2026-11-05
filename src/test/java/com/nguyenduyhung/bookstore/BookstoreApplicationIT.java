package com.nguyenduyhung.bookstore;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test: khởi động TOÀN BỘ ứng dụng với PostgreSQL thật (Testcontainers).
 * Nếu test này pass nghĩa là: app khởi động được, kết nối DB được, Flyway chạy
 * được, Hibernate validate schema thành công và Swagger hoạt động.
 */
class BookstoreApplicationIT extends AbstractIntegrationTest {

    @Test
    void applicationStarts_andOpenApiDocsArePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Bookstore API"));
    }

    @Test
    void healthEndpoint_isPublicAndUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    /**
     * Từ GĐ 1, mọi URL không nằm trong danh sách public đều cần đăng nhập, kể cả
     * URL không tồn tại: người lạ nhận 401 chứ không phải 404, nên không dò được
     * API có những endpoint nào. (Trường hợp đã đăng nhập thì nhận 404, xem AuthIT.)
     */
    @Test
    void unknownEndpoint_withoutToken_returnsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
