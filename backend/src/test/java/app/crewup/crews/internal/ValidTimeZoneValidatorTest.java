package app.crewup.crews.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ValidTimeZoneValidatorTest {

    private final ValidTimeZoneValidator validator = new ValidTimeZoneValidator();

    @ParameterizedTest
    @ValueSource(strings = {"America/Chicago", "Europe/Berlin", "UTC"})
    void M1_1_ianaTimeZonesAreValid(String zone) {
        assertThat(validator.isValid(zone, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"+02:00", "GMT+2", "Mars/Base", "america/chicago", ""})
    void M1_1_offsetsAndUnknownNamesAreInvalid(String zone) {
        assertThat(validator.isValid(zone, null)).isFalse();
    }

    @ParameterizedTest
    @NullSource
    void M1_1_missingTimeZoneIsLeftToNotBlank(String zone) {
        assertThat(validator.isValid(zone, null)).isTrue();
    }
}
