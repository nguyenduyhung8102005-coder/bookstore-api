package com.nguyenduyhung.bookstore.cart;

import com.nguyenduyhung.bookstore.cart.dto.AddCartItemRequest;
import com.nguyenduyhung.bookstore.cart.dto.CartResponse;
import com.nguyenduyhung.bookstore.cart.dto.UpdateCartItemRequest;
import com.nguyenduyhung.bookstore.common.config.OpenApiConfig;
import com.nguyenduyhung.bookstore.common.response.ApiResponse;
import com.nguyenduyhung.bookstore.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Giỏ của tôi": không có id giỏ hay id user trên URL. Server tự biết giỏ của ai
 * từ access token. Đường dẫn dùng bookId thay vì id của dòng cart_items, vì client
 * vốn đã biết bookId và nhờ vậy không cần lộ id nội bộ.
 */
@Tag(name = "Cart")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "Xem giỏ hàng")
    @GetMapping
    public ApiResponse<CartResponse> getCart(@AuthenticationPrincipal AuthUser currentUser) {
        return ApiResponse.success(cartService.getCart(currentUser.id()));
    }

    /** Trả 200 (không phải 201) vì thêm sách đã có chỉ cập nhật dòng cũ, không tạo tài nguyên mới. */
    @Operation(summary = "Thêm sách vào giỏ (đã có thì cộng dồn số lượng)")
    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(@AuthenticationPrincipal AuthUser currentUser,
                                             @Valid @RequestBody AddCartItemRequest request) {
        return ApiResponse.success(cartService.addItem(currentUser.id(), request));
    }

    /** PATCH vì chỉ sửa MỘT thuộc tính (quantity) của dòng giỏ hàng. */
    @Operation(summary = "Đổi số lượng của một sách trong giỏ")
    @PatchMapping("/items/{bookId}")
    public ApiResponse<CartResponse> updateQuantity(@AuthenticationPrincipal AuthUser currentUser,
                                                    @PathVariable Long bookId,
                                                    @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.success(cartService.updateQuantity(currentUser.id(), bookId, request.quantity()));
    }

    @Operation(summary = "Bỏ một sách khỏi giỏ")
    @DeleteMapping("/items/{bookId}")
    public ApiResponse<CartResponse> removeItem(@AuthenticationPrincipal AuthUser currentUser,
                                                @PathVariable Long bookId) {
        return ApiResponse.success(cartService.removeItem(currentUser.id(), bookId));
    }

    @Operation(summary = "Xóa toàn bộ giỏ")
    @DeleteMapping
    public ApiResponse<CartResponse> clear(@AuthenticationPrincipal AuthUser currentUser) {
        return ApiResponse.success(cartService.clear(currentUser.id()));
    }
}
