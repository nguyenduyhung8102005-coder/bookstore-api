package com.nguyenduyhung.bookstore.order;

import com.nguyenduyhung.bookstore.common.config.OpenApiConfig;
import com.nguyenduyhung.bookstore.common.response.ApiResponse;
import com.nguyenduyhung.bookstore.common.response.PageResponse;
import com.nguyenduyhung.bookstore.order.dto.AdminOrderSummaryResponse;
import com.nguyenduyhung.bookstore.order.dto.OrderResponse;
import com.nguyenduyhung.bookstore.order.dto.UpdateOrderStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Orders")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @Operation(summary = "Danh sách mọi đơn hàng (mới nhất trước), lọc theo trạng thái nếu có")
    @GetMapping
    public ApiResponse<PageResponse<AdminOrderSummaryResponse>> search(
            @RequestParam(required = false) OrderStatus status,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(orderService.searchForAdmin(status, pageable));
    }

    @Operation(summary = "Chi tiết một đơn hàng bất kỳ")
    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(orderService.getForAdmin(id));
    }

    /**
     * PATCH vì chỉ sửa một thuộc tính (status) của đơn. Luật chuyển trạng thái vẫn
     * được kiểm tra đầy đủ: admin không thể, ví dụ, đưa đơn COMPLETED về SHIPPING.
     */
    @Operation(summary = "Đổi trạng thái đơn: PAID -> SHIPPING -> COMPLETED, hoặc hủy (hoàn kho)")
    @PatchMapping("/{id}/status")
    public ApiResponse<OrderResponse> changeStatus(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ApiResponse.success(orderService.changeStatusByAdmin(id, request.status()), "Order status updated");
    }
}
