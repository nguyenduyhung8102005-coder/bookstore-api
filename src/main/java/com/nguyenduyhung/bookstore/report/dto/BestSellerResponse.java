package com.nguyenduyhung.bookstore.report.dto;

import java.math.BigDecimal;

/**
 * @param revenue tiền thu được từ cuốn sách này (theo giá lúc bán), không phải giá hiện tại
 */
public record BestSellerResponse(int rank, Long bookId, String title, long quantitySold, BigDecimal revenue) {
}
