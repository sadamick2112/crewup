package app.crewup.shared;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** One page of a list (API-7). Pass {@code nextCursor} back as {@code cursor} to get the next page. */
public record CursorPage<T>(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<T> items,

        @Schema(
                types = {"string", "null"},
                requiredMode = Schema.RequiredMode.REQUIRED,
                description = "Null on the last page")
        String nextCursor) {}
