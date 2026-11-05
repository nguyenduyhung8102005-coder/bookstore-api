package com.nguyenduyhung.bookstore.auth;

import com.jayway.jsonpath.JsonPath;
import com.nguyenduyhung.bookstore.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm tra toàn bộ luồng xác thực qua HTTP thật (MockMvc), với Spring Security,
 * Flyway và PostgreSQL thật.
 */
class AuthIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Secret@123";

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    // ===================== Đăng ký & đăng nhập =====================

    @Test
    void register_login_thenAccessCurrentUser() throws Exception {
        String email = uniqueEmail();

        register(email)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.role").value("USER"))
                // Không bao giờ được trả mật khẩu hay hash ra ngoài
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        Tokens tokens = login(email, PASSWORD);

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens.access())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.fullName").value("Test User"));
    }

    @Test
    void register_sameEmailWithDifferentCase_isRejected() throws Exception {
        String email = uniqueEmail();
        register(email).andExpect(status().isCreated());

        register(email.toUpperCase())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void register_invalidBody_returnsAllFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "not-an-email", "password": "123", "fullName": "" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("email", "password", "fullName")));
    }

    @Test
    void login_wrongPassword_isRejected() throws Exception {
        String email = uniqueEmail();
        register(email).andExpect(status().isCreated());

        performLogin(email, "Wrong@123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    // ===================== Refresh token =====================

    @Test
    void refresh_issuesNewTokens_andOldRefreshTokenCannotBeReused() throws Exception {
        Tokens first = registerAndLogin();

        Tokens second = readTokens(performRefresh(first.refresh()).andExpect(status().isOk()));
        assertThat(second.refresh()).isNotEqualTo(first.refresh());

        // Dùng lại token cũ: bị từ chối...
        performRefresh(first.refresh())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        // ...và vì bị coi là dấu hiệu token bị đánh cắp, cả token mới cũng bị thu hồi.
        performRefresh(second.refresh())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_revokesRefreshToken() throws Exception {
        Tokens tokens = registerAndLogin();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(tokens.refresh())))
                .andExpect(status().isOk());

        performRefresh(tokens.refresh())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    // ===================== 401 / 403 =====================

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void protectedEndpoint_withInvalidToken_returns401WithSpecificCode() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer("invalid.token.value")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    void userRole_cannotAccessAdminEndpoints() throws Exception {
        Tokens tokens = registerAndLogin();

        mockMvc.perform(get("/api/v1/admin/anything").header(HttpHeaders.AUTHORIZATION, bearer(tokens.access())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    /**
     * GĐ 1 chưa có endpoint admin nào. Nhận 404 (thay vì 403) chứng tỏ ADMIN đã
     * vượt qua lớp phân quyền và request đi tới được DispatcherServlet.
     * Đồng thời xác nhận tài khoản ADMIN đầu tiên đã được tạo lúc khởi động.
     */
    @Test
    void adminRole_passesAdminAuthorization() throws Exception {
        Tokens tokens = login(adminEmail, adminPassword);

        mockMvc.perform(get("/api/v1/admin/anything").header(HttpHeaders.AUTHORIZATION, bearer(tokens.access())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENDPOINT_NOT_FOUND"));
    }

    // ===================== Helper =====================

    private record Tokens(String access, String refresh) {
    }

    private Tokens registerAndLogin() throws Exception {
        String email = uniqueEmail();
        register(email).andExpect(status().isCreated());
        return login(email, PASSWORD);
    }

    private ResultActions register(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "%s", "password": "%s", "fullName": "Test User" }
                        """.formatted(email, PASSWORD)));
    }

    private Tokens login(String email, String password) throws Exception {
        return readTokens(performLogin(email, password).andExpect(status().isOk()));
    }

    private ResultActions performLogin(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "%s", "password": "%s" }
                        """.formatted(email, password)));
    }

    private ResultActions performRefresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(refreshToken)));
    }

    private static Tokens readTokens(ResultActions result) throws Exception {
        String json = result.andReturn().getResponse().getContentAsString();
        return new Tokens(JsonPath.read(json, "$.data.accessToken"), JsonPath.read(json, "$.data.refreshToken"));
    }

    private static String refreshBody(String refreshToken) {
        return """
                { "refreshToken": "%s" }
                """.formatted(refreshToken);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
