package app.crewup.crews.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class MonthInRangeValidatorTest {

    private final MonthInRangeValidator validator = new MonthInRangeValidator();

    @Test
    void M1_1_monthsFrom1900ToThisMonthAreValid() {
        assertThat(validator.isValid(YearMonth.of(1900, 1), null)).isTrue();
        assertThat(validator.isValid(YearMonth.of(1999, 10), null)).isTrue();
        assertThat(validator.isValid(YearMonth.now(), null)).isTrue();
    }

    @Test
    void M1_1_monthsBefore1900OrInTheFutureAreInvalid() {
        assertThat(validator.isValid(YearMonth.of(1899, 12), null)).isFalse();
        // Two months ahead, so the test can't land on a month boundary in some time zone.
        assertThat(validator.isValid(YearMonth.now().plusMonths(2), null)).isFalse();
    }

    @Test
    void M1_1_missingMonthIsLeftToNotNull() {
        assertThat(validator.isValid(null, null)).isTrue();
    }
}
