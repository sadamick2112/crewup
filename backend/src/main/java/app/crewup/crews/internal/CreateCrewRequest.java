package app.crewup.crews.internal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.YearMonth;

/** A new crew (M1-1, GRP-1), plus the creator's first name for their Member profile in it (M1-2). */
record CreateCrewRequest(
        @NotBlank @Size(max = 80) String name,

        @Schema(type = "string", pattern = "^\\d{4}-\\d{2}$", example = "1999-10") @NotNull @MonthInRange
        YearMonth establishedMonth,

        @NotNull CrewIcon iconKey,

        @Schema(example = "America/Chicago") @NotBlank @ValidTimeZone
        String timeZone,

        @Size(max = 150) String motto,

        @Size(max = 2000) String description,

        @NotBlank @Size(max = 50) String creatorFirstName) {}
