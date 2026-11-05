package com.nguyenduyhung.bookstore.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Lớp cha cho mọi entity có khóa chính tự tăng và thời điểm tạo/sửa.
 *
 * - @MappedSuperclass: lớp này KHÔNG phải một bảng; các field của nó được
 *   "chép" vào bảng của từng entity con.
 * - AuditingEntityListener: trước khi INSERT/UPDATE, Spring Data tự gán
 *   createdAt / updatedAt, nên code nghiệp vụ không phải nhớ set tay.
 * - Không có setter cho các field này: chỉ JPA và cơ chế auditing được ghi.
 *
 * Dùng Instant (thời điểm tuyệt đối, UTC) map sang cột TIMESTAMPTZ trong
 * PostgreSQL, tránh lỗi lệch múi giờ giữa server, DB và client.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /*
     * IDENTITY: DB tự sinh id khi INSERT. Đơn giản, khớp với cột IDENTITY của PostgreSQL.
     * Đánh đổi: Hibernate phải INSERT ngay để lấy id, nên không gộp được nhiều
     * INSERT thành một batch. Phương án SEQUENCE cho phép batch insert nhưng
     * phức tạp hơn; với quy mô dự án này IDENTITY là đủ.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
