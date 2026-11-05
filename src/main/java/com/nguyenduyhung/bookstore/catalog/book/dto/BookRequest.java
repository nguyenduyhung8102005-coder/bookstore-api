package com.nguyenduyhung.bookstore.catalog.book.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Dùng cho cả tạo mới (POST) và cập nhật (PUT). PUT nghĩa là THAY THẾ toàn bộ
 * tài nguyên, nên client phải gửi đủ mọi field bắt buộc như khi tạo mới.
 *
 * Dùng kiểu bao (Integer, BigDecimal) thay vì kiểu nguyên thủy (int): nếu client
 * quên gửi stockQuantity, int sẽ âm thầm thành 0, còn Integer là null và bị
 * @NotNull báo lỗi rõ ràng.
 */
public record BookRequest(
        @NotBlank @Size(max = 255)
        String title,

        // Chỉ nhận chữ số, không gạch nối: ISBN-10 (9 số + số/X) hoặc ISBN-13.
        // Chưa kiểm tra chữ số kiểm tra (checksum); có thể bổ sung bằng custom validator.
        @NotBlank
        @Pattern(regexp = "^(\\d{9}[\\dX]|\\d{13})$", message = "must be ISBN-10 or ISBN-13, digits only")
        String isbn,

        @Size(max = 10_000)
        String description,

        @NotNull
        @DecimalMin(value = "0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @NotNull @PositiveOrZero
        Integer stockQuantity,

        @Min(1450) @Max(2100)
        Integer publishedYear,

        @NotNull
        Long categoryId,

        // @NotNull bên trong <> kiểm tra TỪNG phần tử, chặn mảng kiểu [1, null].
        @NotEmpty @Size(max = 10)
        Set<@NotNull Long> authorIds
) {
}
