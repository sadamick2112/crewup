package app.crewup.crews.internal;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A month from January 1900 to the current month, such as a crew's "established" date (GRP-1). Null is valid. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MonthInRangeValidator.class)
@interface MonthInRange {

    String message() default "must be between January 1900 and this month";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
