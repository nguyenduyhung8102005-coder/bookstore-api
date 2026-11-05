# Bookstore API

REST API cho hệ thống bán sách trực tuyến, viết bằng Java 21 và Spring Boot 3.5.

> README này sẽ được viết lại đầy đủ ở Giai đoạn 8 (sơ đồ kiến trúc, quyết định thiết kế, ví dụ request).

## Công nghệ

Java 21 · Spring Boot 3.5 · Spring Data JPA · PostgreSQL 17 · Flyway · Spring Security + JWT · springdoc-openapi · JUnit 5 · Mockito · Testcontainers · Docker · GitHub Actions

## Chạy trên máy

Yêu cầu: JDK 21, Maven 3.9+, Docker.

```bash
# 1. Khởi động PostgreSQL
docker compose up -d

# 2. Chạy ứng dụng
mvn spring-boot:run

# 3. Mở tài liệu API
#    http://localhost:8080/swagger-ui.html
```

## Chạy test

```bash
mvn test     # chỉ unit test (*Test), không cần Docker
mvn verify   # unit test + integration test (*IT), cần Docker đang chạy
```

## Format response

Thành công:

```json
{ "success": true, "code": "SUCCESS", "message": "OK", "data": { }, "timestamp": "2026-11-05T11:00:00Z" }
```

Lỗi:

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "errors": [ { "field": "email", "message": "must be a well-formed email address" } ],
  "timestamp": "2026-11-05T11:00:00Z"
}
```

## Tiến độ

- [x] Giai đoạn 0: Nền móng (cấu hình, response chuẩn, xử lý lỗi tập trung, Swagger, CI)
- [ ] Giai đoạn 1: Xác thực & phân quyền (JWT + refresh token)
- [ ] Giai đoạn 2: Quản lý sách, tác giả, thể loại
- [ ] Giai đoạn 3: Tìm kiếm, lọc, phân trang, xử lý N+1
- [ ] Giai đoạn 4: Giỏ hàng
- [ ] Giai đoạn 5: Đặt hàng, tồn kho, optimistic locking
- [ ] Giai đoạn 6: Đánh giá sách
- [ ] Giai đoạn 7: Thống kê cho admin
- [ ] Giai đoạn 8: Docker hóa, hoàn thiện README
