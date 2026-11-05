package com.nguyenduyhung.bookstore.cart.dto;

import java.math.BigDecimal;

/**
 * @param unitPrice giá HIỆN TẠI của sách. Giỏ hàng không "giữ giá": admin đổi giá
 *                  thì giỏ hiển thị giá mới. Giá chỉ được chốt lúc tạo đơn (GĐ 5).
 * @param available false nếu sách đã ngừng bán hoặc tồn kho không đủ số lượng trong
 *                  giỏ. Dòng đó vẫn được giữ để người dùng tự quyết định sửa hay xóa.
 */
public record CartItemResponse(
        Long bookId,
        String title,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        boolean available
) {
}
