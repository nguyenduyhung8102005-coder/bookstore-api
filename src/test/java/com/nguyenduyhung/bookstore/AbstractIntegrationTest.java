package com.nguyenduyhung.bookstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lớp cha của MỌI integration test.
 *
 * Spring cache lại ApplicationContext giữa các class test nếu cấu hình của chúng
 * GIỐNG HỆT nhau. Gom cấu hình về một chỗ đảm bảo tất cả *IT dùng chung một
 * context và một container PostgreSQL, thay vì mỗi class khởi động lại từ đầu
 * (chậm hơn nhiều lần). Lưu ý: thêm @MockitoBean hay properties riêng ở class con
 * sẽ tạo ra context mới.
 *
 * Vì dùng chung DB, mỗi test phải tự tạo dữ liệu riêng (ví dụ email, ISBN ngẫu
 * nhiên) để các test không ảnh hưởng lẫn nhau.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final String TEST_USER_PASSWORD = "Secret@123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Value("${app.admin.email}")
    private String defaultAdminEmail;

    @Value("${app.admin.password}")
    private String defaultAdminPassword;

    /** Access token của tài khoản ADMIN được tạo sẵn lúc khởi động. */
    protected String adminAccessToken() throws Exception {
        return accessTokenFor(defaultAdminEmail, defaultAdminPassword);
    }

    /** Đăng ký một USER mới (email ngẫu nhiên) và trả về access token của user đó. */
    protected String newUserAccessToken() throws Exception {
        String email = "it-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", email, "password", TEST_USER_PASSWORD, "fullName", "IT User"))))
                .andExpect(status().isCreated());
        return accessTokenFor(email, TEST_USER_PASSWORD);
    }

    /**
     * Tạo nhanh một cuốn sách đang bán (kèm thể loại và tác giả mới) bằng tài khoản
     * ADMIN, dùng cho các test cần có sách sẵn (giỏ hàng, đơn hàng...).
     *
     * @return id của sách vừa tạo
     */
    protected Long createSampleBook(String price, int stockQuantity) throws Exception {
        String adminToken = adminAccessToken();
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Long categoryId = postAsAdmin(adminToken, "/api/v1/admin/categories", Map.of("name", "Sample " + suffix));
        Long authorId = postAsAdmin(adminToken, "/api/v1/admin/authors", Map.of("name", "Author " + suffix));

        Map<String, Object> book = new HashMap<>();
        book.put("title", "Sample book " + suffix);
        book.put("isbn", "978" + ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
        book.put("price", new BigDecimal(price));
        book.put("stockQuantity", stockQuantity);
        book.put("categoryId", categoryId);
        book.put("authorIds", List.of(authorId));
        return postAsAdmin(adminToken, "/api/v1/admin/books", book);
    }

    private Long postAsAdmin(String adminToken, String url, Object body) throws Exception {
        return dataId(mockMvc.perform(post(url)
                        .header(HttpHeaders.AUTHORIZATION, authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(body)))
                .andExpect(status().isCreated()));
    }

    protected static String authHeader(String accessToken) {
        return "Bearer " + accessToken;
    }

    protected String toJson(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    /** Đọc một giá trị trong JSON response, ví dụ jsonValue(result, "$.data.id"). */
    protected static <T> T jsonValue(ResultActions result, String jsonPath) throws Exception {
        // Đọc theo UTF-8 tường minh: mặc định MockHttpServletResponse dùng ISO-8859-1,
        // làm hỏng tiếng Việt có dấu.
        String body = result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, jsonPath);
    }

    /** Lấy id từ "$.data.id". JSON số nhỏ được đọc thành Integer nên phải đổi sang Long. */
    protected static Long dataId(ResultActions result) throws Exception {
        Number id = jsonValue(result, "$.data.id");
        return id.longValue();
    }

    private String accessTokenFor(String email, String password) throws Exception {
        ResultActions result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", email, "password", password))))
                .andExpect(status().isOk());
        return jsonValue(result, "$.data.accessToken");
    }
}
