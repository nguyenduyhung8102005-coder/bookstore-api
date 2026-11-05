package com.nguyenduyhung.bookstore.catalog.book;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phần lọc theo từng tiêu chí được kiểm tra với PostgreSQL thật trong BookSearchIT.
 * Ở đây chỉ unit test hàm escape thuần Java.
 */
class BookSpecificationsTest {

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource(delimiter = '|', value = {
            "dế mèn      | dế mèn",
            "50%         | 50\\%",
            "a_b         | a\\_b",
            "100%_off    | 100\\%\\_off",
            "C:\\temp    | C:\\\\temp"
    })
    void escapeLike_escapesWildcardsAndEscapeCharacter(String input, String expected) {
        assertThat(BookSpecifications.escapeLike(input)).isEqualTo(expected);
    }
}
