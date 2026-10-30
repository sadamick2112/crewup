package app.crewup.shared;

import io.swagger.v3.oas.annotations.media.Schema;

/** One entry in a {@code 400} response's {@code errors} array. */
public record InvalidField(
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description = "The request property's path, such as socialLinks[0].url; empty for the whole request")
        String field,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String message) {}
