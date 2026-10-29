package app.crewup.crews.internal;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.YearMonth;
import java.util.UUID;

/** A crew with every field, plus the caller's own membership, so the UI knows which actions to offer (M1-4). */
record CrewDetails(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,

        @Schema(type = "string", pattern = "^\\d{4}-\\d{2}$", requiredMode = Schema.RequiredMode.REQUIRED)
        YearMonth establishedMonth,

        @Schema(
                types = {"string", "null"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        String motto,

        @Schema(
                types = {"string", "null"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        String description,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CrewIcon iconKey,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "America/Chicago")
        String timeZone,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The caller's Member id in this crew")
        UUID myMemberId,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The caller's role in this crew")
        MemberRole myRole) {}
