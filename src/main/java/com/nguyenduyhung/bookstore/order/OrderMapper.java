package com.nguyenduyhung.bookstore.order;

import com.nguyenduyhung.bookstore.order.dto.OrderItemResponse;
import com.nguyenduyhung.bookstore.order.dto.OrderResponse;
import com.nguyenduyhung.bookstore.order.dto.OrderSummaryResponse;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getRecipientName(),
                order.getPhone(),
                order.getShippingAddress(),
                order.getItems().stream().map(OrderMapper::toItemResponse).toList(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getCancelledAt());
    }

    public static OrderSummaryResponse toSummary(Order order) {
        return new OrderSummaryResponse(
                order.getId(), order.getStatus(), order.getTotalAmount(), order.totalItems(), order.getCreatedAt());
    }

    private static OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getBookId(), item.getBookTitle(), item.getUnitPrice(), item.getQuantity(), item.lineTotal());
    }
}
