package app.crewup.crews.internal;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.YearMonth;

class MonthInRangeValidator implements ConstraintValidator<MonthInRange, YearMonth> {

    @Override
    public boolean isValid(YearMonth value, ConstraintValidatorContext context) {
        throw new UnsupportedOperationException("not implemented");
    }
}
