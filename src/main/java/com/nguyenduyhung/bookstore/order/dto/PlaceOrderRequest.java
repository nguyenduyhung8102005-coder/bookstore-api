package com.nguyenduyhung.bookstore.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Thông tin giao hàng. Danh sách sách KHÔNG nằm trong request: đơn luôn được tạo
 * từ giỏ hàng trên server, nên client không thể tự gửi giá hay số lượng tùy ý.
 */
public record PlaceOrderRequest(
        @NotBlank @Size(max = 100)
        String recipientName,

        // Số điện thoại Việt Nam: bắt đầu bằng 0 hoặc +84, theo sau 9 chữ số.
        @NotBlank
        @Pattern(regexp = "^(0|\\+84)\\d{9}$", message = "must be a valid Vietnamese phone number")
        String phone,

        @NotBlank @Size(max = 500)
        String shippingAddress
) {
}
