package app.crewup.crews.internal;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.ZoneId;

class ValidTimeZoneValidator implements ConstraintValidator<ValidTimeZone, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Region IDs only: ZoneId.of would also accept fixed offsets such as +02:00.
        return value == null || ZoneId.getAvailableZoneIds().contains(value);
    }
}
