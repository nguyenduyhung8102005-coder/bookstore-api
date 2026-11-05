package com.nguyenduyhung.bookstore.common.exception;

import com.nguyenduyhung.bookstore.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit test cho GlobalExceptionHandler.
 *
 * Dùng MockMvc ở chế độ "standalone": chỉ dựng đúng 1 controller giả và advice
 * cần test, KHÔNG khởi động Spring context hay database. Vì vậy test chạy rất
 * nhanh và không cần Docker.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FakeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Response thành công có success=true, code=SUCCESS và không có field errors")
    void successResponse_hasUnifiedFormat() throws Exception {
        mockMvc.perform(get("/fake/ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.value").value("hello"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("AppException được map sang đúng HTTP status, code và message")
    void appException_isMappedToItsErrorCode() throws Exception {
        mockMvc.perform(get("/fake/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Book not found with id: 99"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("Body không hợp lệ trả 400 kèm danh sách lỗi theo từng field")
    void invalidBody_returnsFieldErrors() throws Exception {
        mockMvc.perform(post("/fake/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "", "quantity": 0 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("name", "quantity")));
    }

    @Test
    @DisplayName("JSON hỏng cú pháp trả 400 INVALID_REQUEST_BODY")
    void malformedJson_returnsInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/fake/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));
    }

    @Test
    @DisplayName("Path variable sai kiểu trả 400 INVALID_PARAMETER")
    void wrongPathVariableType_returnsInvalidParameter() throws Exception {
        mockMvc.perform(get("/fake/items/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.message").value("Parameter 'id' has invalid value 'abc'"));
    }

    @Test
    @DisplayName("Sai HTTP method trả 405 METHOD_NOT_ALLOWED")
    void wrongHttpMethod_returnsMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/fake/ok"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("AccessDeniedException từ @PreAuthorize trả 403 chứ không phải 500")
    void accessDenied_returnsForbidden() throws Exception {
        mockMvc.perform(get("/fake/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Lỗi không lường trước trả 500 và KHÔNG làm lộ chi tiết nội bộ")
    void unexpectedException_hidesInternalDetails() throws Exception {
        mockMvc.perform(get("/fake/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_ERROR.getDefaultMessage()));
    }

    // ---------- Controller giả, chỉ tồn tại trong test ----------

    @RestController
    @RequestMapping("/fake")
    public static class FakeController {

        @GetMapping("/ok")
        public ApiResponse<Map<String, String>> ok() {
            return ApiResponse.success(Map.of("value", "hello"));
        }

        @GetMapping("/not-found")
        public void notFound() {
            throw AppException.notFound("Book", 99);
        }

        @PostMapping("/validate")
        public void validate(@Valid @RequestBody SampleRequest request) {
            // Không làm gì: chỉ cần Spring validate request.
        }

        @GetMapping("/items/{id}")
        public void item(@PathVariable Long id) {
            // Không làm gì: chỉ cần Spring chuyển đổi kiểu của id.
        }

        @GetMapping("/forbidden")
        public void forbidden() {
            throw new AccessDeniedException("Access Denied");
        }

        @GetMapping("/boom")
        public void boom() {
            throw new IllegalStateException("SQL error at table users - internal detail");
        }
    }

    public record SampleRequest(@NotBlank String name, @Min(1) int quantity) {
    }
}
