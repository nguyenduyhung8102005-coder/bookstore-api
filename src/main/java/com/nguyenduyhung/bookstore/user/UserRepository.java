package com.nguyenduyhung.bookstore.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data tự sinh câu SQL từ tên method (derived query):
 * findByEmail -> SELECT ... WHERE email = ?
 * existsByEmail -> SELECT 1 ... WHERE email = ? LIMIT 1 (nhẹ hơn tải cả entity)
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);
}
