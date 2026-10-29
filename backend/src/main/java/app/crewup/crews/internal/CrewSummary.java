package app.crewup.crews.internal;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.YearMonth;
import java.util.UUID;

/** One of the caller's crews, as listed on My crews and in the switcher (M1-3). */
record CrewSummary(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CrewIcon iconKey,

        @Schema(
                types = {"string", "null"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        String motto,

        @Schema(type = "string", pattern = "^\\d{4}-\\d{2}$", requiredMode = Schema.RequiredMode.REQUIRED)
        YearMonth establishedMonth,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The caller's role in this crew")
        MemberRole myRole) {}
