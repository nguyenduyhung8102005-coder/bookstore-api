package com.nguyenduyhung.bookstore.order;

import com.nguyenduyhung.bookstore.common.config.OpenApiConfig;
import com.nguyenduyhung.bookstore.common.response.ApiResponse;
import com.nguyenduyhung.bookstore.common.response.PageResponse;
import com.nguyenduyhung.bookstore.order.dto.OrderResponse;
import com.nguyenduyhung.bookstore.order.dto.OrderSummaryResponse;
import com.nguyenduyhung.bookstore.order.dto.PlaceOrderRequest;
import com.nguyenduyhung.bookstore.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Orders")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Đặt hàng từ toàn bộ giỏ hàng hiện tại")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> placeOrder(@AuthenticationPrincipal AuthUser currentUser,
                                                 @Valid @RequestBody PlaceOrderRequest request) {
        return ApiResponse.success(orderService.placeOrder(currentUser.id(), request), "Order placed");
    }

    @Operation(summary = "Đơn hàng của tôi (mới nhất trước)")
    @GetMapping
    public ApiResponse<PageResponse<OrderSummaryResponse>> getMyOrders(
            @AuthenticationPrincipal AuthUser currentUser,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(orderService.getMyOrders(currentUser.id(), pageable));
    }

    @Operation(summary = "Chi tiết một đơn hàng của tôi")
    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getMyOrder(@AuthenticationPrincipal AuthUser currentUser,
                                                 @PathVariable Long id) {
        return ApiResponse.success(orderService.getMyOrder(currentUser.id(), id));
    }
}
