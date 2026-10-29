package app.crewup.crews.internal;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.YearMonth;
import java.time.ZoneOffset;

class MonthInRangeValidator implements ConstraintValidator<MonthInRange, YearMonth> {

    private static final YearMonth EARLIEST = YearMonth.of(1900, 1);
    // The latest time zone (UTC+14) starts a new month first, so a month that has begun anywhere is accepted.
    private static final ZoneOffset LATEST_ZONE = ZoneOffset.ofHours(14);

    @Override
    public boolean isValid(YearMonth value, ConstraintValidatorContext context) {
        return value == null || (!value.isBefore(EARLIEST) && !value.isAfter(YearMonth.now(LATEST_ZONE)));
    }
}
