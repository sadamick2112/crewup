package app.crewup.crews.internal;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** An IANA time zone name such as {@code America/Chicago}; fixed offsets such as {@code +02:00} aren't. Null is valid. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidTimeZoneValidator.class)
@interface ValidTimeZone {

    String message() default "must be an IANA time zone, such as America/Chicago";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
