# Bookstore API

REST API cho hệ thống bán sách trực tuyến, viết bằng Java 21 và Spring Boot 3.5.

> README này sẽ được viết lại đầy đủ ở Giai đoạn 8 (sơ đồ kiến trúc, quyết định thiết kế, ví dụ request).

## Công nghệ

Java 21 · Spring Boot 3.5 · Spring Data JPA · PostgreSQL 17 · Flyway · Spring Security + JWT · springdoc-openapi · JUnit 5 · Mockito · Testcontainers · Docker · GitHub Actions

## Chạy trên máy

Yêu cầu: **JDK 21** (đúng bản 21, build sẽ dừng nếu Maven chạy bằng JDK khác), Maven 3.9+, Docker.
Kiểm tra bằng `mvn -v`, dòng `Java version` phải là 21.x.

```bash
# 1. Khởi động PostgreSQL
docker compose up -d

# 2. Chạy ứng dụng
mvn spring-boot:run

# 3. Mở tài liệu API
#    http://localhost:8080/swagger-ui.html
```

## Xác thực

- Đăng ký `POST /api/v1/auth/register`, đăng nhập `POST /api/v1/auth/login` để nhận `accessToken` (15 phút) và `refreshToken` (7 ngày).
- Gọi API cần đăng nhập với header `Authorization: Bearer <accessToken>`. Trên Swagger UI: bấm **Authorize** và dán access token.
- Hết hạn access token: gọi `POST /api/v1/auth/refresh` với refresh token để nhận cặp token mới (token cũ bị vô hiệu).
- Khi chạy local, tài khoản ADMIN mặc định được tạo lúc khởi động: `admin@bookstore.local` / `Admin@123456`. Môi trường thật phải đặt `ADMIN_EMAIL`, `ADMIN_PASSWORD` và `JWT_SECRET`.

## Danh mục sách

- Public (không cần token): `GET /api/v1/books`, `GET /api/v1/books/{id}`, `GET /api/v1/categories`, `GET /api/v1/authors`, `GET /api/v1/authors/{id}`.
- Quản trị (role ADMIN): `POST/PUT/DELETE /api/v1/admin/{books|authors|categories}`.
- Xóa sách là soft delete (`active = false`): sách ẩn khỏi API public nhưng vẫn còn trong DB cho đơn hàng cũ tham chiếu.
- Tìm kiếm & lọc: `GET /api/v1/books?keyword=dế mèn&categoryId=1&authorId=2&minPrice=50000&maxPrice=200000`
- Phân trang & sắp xếp: `&page=0&size=20&sort=price,asc` (tối đa 100 phần tử mỗi trang; sắp xếp được theo `createdAt`, `price`, `title`).
- Danh sách sách chạy tối đa 3 câu SQL dù trang có bao nhiêu cuốn (đã xử lý N+1, có test đo bằng Hibernate Statistics).

## Giỏ hàng (cần đăng nhập)

- `GET /api/v1/cart` · `POST /api/v1/cart/items` `{bookId, quantity}` (sách đã có thì cộng dồn) · `PATCH /api/v1/cart/items/{bookId}` `{quantity}` · `DELETE /api/v1/cart/items/{bookId}` · `DELETE /api/v1/cart`
- Mọi thao tác trả về giỏ mới nhất kèm `totalItems`, `totalAmount` và cờ `hasUnavailableItems`.
- Giỏ hàng không giữ hàng và không chốt giá: tồn kho và giá được kiểm tra lại khi đặt hàng.

## Đặt hàng (cần đăng nhập)

- `POST /api/v1/orders` `{recipientName, phone, shippingAddress}`: tạo đơn PENDING từ toàn bộ giỏ hàng, trừ tồn kho, chốt giá, xóa giỏ. Tất cả trong một transaction: một cuốn thiếu hàng thì cả đơn bị hủy.
- `GET /api/v1/orders` (mới nhất trước) · `GET /api/v1/orders/{id}` (đơn của người khác trả 404).
- `POST /api/v1/orders/{id}/pay` (giả lập thanh toán) · `POST /api/v1/orders/{id}/cancel` (hủy và hoàn kho).
- Admin: `GET /api/v1/admin/orders?status=PAID` · `GET /api/v1/admin/orders/{id}` · `PATCH /api/v1/admin/orders/{id}/status` `{status: SHIPPING | COMPLETED | CANCELLED}`.

### Vòng đời đơn hàng

```
PENDING ──pay──> PAID ──ship──> SHIPPING ──complete──> COMPLETED
   │               │
   └──cancel──> CANCELLED <──cancel──┘     (hủy = hoàn kho)
```

Luật chuyển trạng thái nằm ở một chỗ duy nhất (`OrderStatus.canTransitionTo`). Đơn PENDING quá 30 phút chưa thanh toán bị job chạy mỗi phút tự hủy và hoàn kho (`app.order.pending-timeout`).

### Hai người cùng mua cuốn sách cuối cùng

Bảng `books` có cột `version` (`@Version`). Mỗi câu UPDATE đều kèm điều kiện `WHERE version = <giá trị đã đọc>`, nên khi hai đơn cùng trừ kho từ một con số cũ, chỉ đơn commit trước thành công; đơn còn lại nhận `ObjectOptimisticLockingFailureException`, được thử lại tối đa 3 lần trong transaction mới, và lần thử lại sẽ thấy đúng tồn kho mới nhất. Test `OrderConcurrencyIT` cho nhiều luồng đặt hàng cùng lúc và kiểm tra: không bao giờ bán quá số lượng, và `số đã bán + tồn kho còn lại = tồn kho ban đầu`.

| Cách làm | Cơ chế | Ưu điểm | Nhược điểm |
|---|---|---|---|
| **Optimistic lock** (`@Version`) — đang dùng | Không khóa khi đọc; phát hiện xung đột lúc ghi, thử lại | Không giữ khóa lâu; tốt khi xung đột hiếm; bảo vệ mọi thao tác sửa sách (cả admin sửa giá) | Xung đột nhiều thì retry nhiều; phải tự viết retry ngoài transaction |
| **Pessimistic lock** (`SELECT ... FOR UPDATE`) | Khóa dòng ngay khi đọc, người sau phải chờ | Không cần retry, hành vi dễ đoán | Giữ khóa suốt transaction; dễ deadlock nếu khóa nhiều dòng sai thứ tự; giảm khả năng chạy song song |
| **UPDATE có điều kiện** (`UPDATE books SET stock = stock - :q WHERE id = :id AND stock >= :q`) | DB tự kiểm tra và trừ trong một câu nguyên tử | Nhanh nhất, không cần retry, tốt cho "flash sale" | Đi vòng qua entity (phải tự đồng bộ persistence context); chỉ bảo vệ đúng thao tác trừ kho |

Dự án dùng kiểu "UPDATE có điều kiện" ở chỗ khác: thu hồi refresh token (`RefreshTokenRepository.revokeIfActive`).

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
- [x] Giai đoạn 1: Xác thực & phân quyền (JWT + refresh token)
- [x] Giai đoạn 2: Quản lý sách, tác giả, thể loại
- [x] Giai đoạn 3: Tìm kiếm, lọc, phân trang, xử lý N+1
- [x] Giai đoạn 4: Giỏ hàng
- [x] Giai đoạn 5a: Đặt hàng, trừ tồn kho an toàn khi mua đồng thời (optimistic locking + retry)
- [x] Giai đoạn 5b: Thanh toán, hủy đơn, trạng thái đơn, tự hủy đơn quá hạn
- [ ] Giai đoạn 6: Đánh giá sách
- [ ] Giai đoạn 7: Thống kê cho admin
- [ ] Giai đoạn 8: Docker hóa, hoàn thiện README
