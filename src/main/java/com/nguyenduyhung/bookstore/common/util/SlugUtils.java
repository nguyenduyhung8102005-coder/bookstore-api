package com.nguyenduyhung.bookstore.common.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Chuyển chuỗi bất kỳ thành "slug" dùng được trong URL: "Văn học Việt Nam" -> "van-hoc-viet-nam".
 */
public final class SlugUtils {

    private SlugUtils() {
    }

    public static String toSlug(String input) {
        // 1. Viết thường trước, để "Đ" thành "đ", rồi thay "đ" bằng "d".
        //    "đ" là một ký tự riêng chứ không phải "d" + dấu, nên bước 2 không bỏ được.
        String result = input.trim().toLowerCase(Locale.ROOT).replace('đ', 'd');

        // 2. Chuẩn hóa NFD tách ký tự có dấu thành ký tự gốc + dấu riêng
        //    ("ệ" -> "e" + dấu mũ + dấu nặng), rồi xóa toàn bộ các dấu (\p{M}).
        result = Normalizer.normalize(result, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");

        // 3. Mọi cụm ký tự không phải chữ/số thành một dấu "-", bỏ "-" ở hai đầu.
        return result.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
    }
}
