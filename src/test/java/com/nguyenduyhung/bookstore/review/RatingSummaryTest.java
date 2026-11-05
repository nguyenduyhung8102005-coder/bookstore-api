package com.nguyenduyhung.bookstore.review;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RatingSummaryTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "4.25, 4.3",               // HALF_UP: đúng nửa thì làm tròn lên
            "4.24, 4.2",
            "4.333333333, 4.3",        // ví dụ (4 + 4 + 5) / 3
            "5.0, 5.0",
            "1.0, 1.0"
    })
    void roundedAverage_hasOneDecimalPlace(double average, String expected) {
        assertThat(new RatingSummary(average, 3L).roundedAverage()).isEqualTo(new BigDecimal(expected));
    }

    @Test
    void noReviews_averageIsNull() {
        assertThat(RatingSummary.EMPTY.roundedAverage()).isNull();
        assertThat(RatingSummary.EMPTY.count()).isZero();
    }
}
