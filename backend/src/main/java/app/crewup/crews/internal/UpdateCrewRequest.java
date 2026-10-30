package app.crewup.crews.internal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.YearMonth;

/** Every crew field, with the same rules as when it was created (M1-5, GRP-2). */
record UpdateCrewRequest(
        @NotBlank @Size(max = 80) String name,

        @Schema(type = "string", pattern = "^\\d{4}-\\d{2}$", example = "1999-10") @NotNull @MonthInRange
        YearMonth establishedMonth,

        @NotNull CrewIcon iconKey,

        @Schema(example = "America/Chicago") @NotBlank @ValidTimeZone
        String timeZone,

        @Size(max = 150) String motto,

        @Size(max = 2000) String description) {}
