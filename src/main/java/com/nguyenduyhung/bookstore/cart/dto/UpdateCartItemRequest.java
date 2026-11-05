package com.nguyenduyhung.bookstore.cart.dto;

import com.nguyenduyhung.bookstore.cart.CartItem;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Số lượng MỚI (đặt lại, không cộng dồn). Tối thiểu là 1: muốn bỏ sách khỏi giỏ
 * thì gọi DELETE, để mỗi endpoint chỉ làm đúng một việc rõ ràng.
 */
public record UpdateCartItemRequest(
        @NotNull @Min(1) @Max(CartItem.MAX_QUANTITY)
        Integer quantity
) {
}
