package com.nguyenduyhung.bookstore.common.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SlugUtilsTest {

    /**
     * @ParameterizedTest: chạy cùng một test với nhiều bộ dữ liệu.
     * Mỗi dòng CsvSource là một lần chạy: input | kết quả mong đợi.
     */
    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource(delimiter = '|', value = {
            "Văn học Việt Nam            | van-hoc-viet-nam",
            "Đời sống & Xã hội           | doi-song-xa-hoi",
            "  Khoa học   viễn tưởng!!   | khoa-hoc-vien-tuong",
            "Tâm lý - Kỹ năng sống       | tam-ly-ky-nang-song",
            "Ngoại ngữ: Tiếng Anh 2026   | ngoai-ngu-tieng-anh-2026",
            "Thiếu nhi                   | thieu-nhi",
            "Lịch sử – Địa lý            | lich-su-dia-ly"
    })
    void toSlug_removesVietnameseAccentsAndSymbols(String input, String expected) {
        assertThat(SlugUtils.toSlug(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"!!!", "' - '"})
    void toSlug_ofStringWithoutLettersOrDigits_isEmpty(String input) {
        assertThat(SlugUtils.toSlug(input)).isEmpty();
    }
}
