package com.nguyenduyhung.bookstore.review;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Điểm trung bình và số lượt đánh giá của một cuốn sách, tính bằng MỘT câu SQL
 * (AVG + COUNT) ngay trong DB, thay vì tải mọi đánh giá về Java rồi mới tính.
 *
 * Kiểu Double và Long (không phải double và long) vì đây là đúng kiểu mà JPQL trả
 * về cho avg() và count(); constructor expression trong ReviewRepository yêu cầu khớp kiểu.
 *
 * @param average null khi sách chưa có đánh giá nào (AVG của tập rỗng là NULL)
 */
public record RatingSummary(Double average, Long count) {

    public static final RatingSummary EMPTY = new RatingSummary(null, 0L);

    /**
     * Làm tròn 1 chữ số thập phân (4.25 -> 4.3). Phép chia trong AVG cho ra số lẻ
     * vô hạn kiểu 4.333..., nên đây là lúc BẮT BUỘC chỉ định RoundingMode.
     */
    public BigDecimal roundedAverage() {
        return average == null ? null : BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP);
    }
}
