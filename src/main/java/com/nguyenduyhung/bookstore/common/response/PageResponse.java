package com.nguyenduyhung.bookstore.common.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Bọc kết quả phân trang thành một cấu trúc JSON ổn định.
 *
 * Vì sao không trả thẳng Page<T> của Spring Data?
 * JSON của PageImpl chứa nhiều field nội bộ (pageable, sort, ...) và Spring Data
 * không cam kết giữ nguyên cấu trúc đó giữa các phiên bản (từ 3.3 còn in cảnh
 * báo khi serialize PageImpl). Tự định nghĩa DTO thì API của ta ổn định và gọn.
 *
 * Lưu ý: `page` đánh số từ 0, giống Spring Data.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
