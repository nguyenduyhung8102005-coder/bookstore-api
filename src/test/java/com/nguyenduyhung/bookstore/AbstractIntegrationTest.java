package com.nguyenduyhung.bookstore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Lớp cha của MỌI integration test.
 *
 * Spring cache lại ApplicationContext giữa các class test nếu cấu hình của chúng
 * GIỐNG HỆT nhau. Gom cấu hình về một chỗ đảm bảo tất cả *IT dùng chung một
 * context và một container PostgreSQL, thay vì mỗi class khởi động lại từ đầu
 * (chậm hơn nhiều lần). Lưu ý: thêm @MockitoBean hay properties riêng ở class con
 * sẽ tạo ra context mới.
 *
 * Vì dùng chung DB, mỗi test phải tự tạo dữ liệu riêng (ví dụ email ngẫu nhiên)
 * để các test không ảnh hưởng lẫn nhau.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;
}
