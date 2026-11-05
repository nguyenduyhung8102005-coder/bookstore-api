package com.nguyenduyhung.bookstore.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Tạo tài khoản ADMIN đầu tiên khi ứng dụng khởi động, nếu DB chưa có ADMIN nào.
 *
 * Vì sao không chèn ADMIN bằng migration SQL?
 * Migration phải chứa sẵn hash mật khẩu, tức mật khẩu admin bị "đóng băng" trong
 * mã nguồn và lịch sử Git, mọi môi trường dùng chung một mật khẩu. Cách này đọc
 * email/mật khẩu từ biến môi trường, mỗi môi trường đặt giá trị riêng.
 *
 * ApplicationRunner chạy SAU khi context khởi động xong, tức Flyway đã tạo bảng.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        if (!StringUtils.hasText(adminProperties.email()) || !StringUtils.hasText(adminProperties.password())) {
            log.warn("No ADMIN account exists and app.admin.email / app.admin.password are not set");
            return;
        }
        User admin = User.create(
                adminProperties.email(),
                passwordEncoder.encode(adminProperties.password()),
                "Administrator",
                Role.ADMIN);
        userRepository.save(admin);
        log.info("Created initial ADMIN account: {}", admin.getEmail());
    }
}
