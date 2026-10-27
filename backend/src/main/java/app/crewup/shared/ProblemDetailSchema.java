package app.crewup.shared;

import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;

/**
 * Documents the error body every endpoint returns (RFC 9457 Problem Details, API-4), for use in {@code @ApiResponse}.
 * Documentation only: responses are built as Spring's {@code ProblemDetail}, whose Java shape (a nested
 * {@code properties} map) differs from the JSON it writes, where {@code traceId} is a top-level field (M0-27).
 */
@Schema(name = "ProblemDetail", description = "An error, as RFC 9457 Problem Details")
public record ProblemDetailSchema(
        @Schema(description = "Identifies the kind of error, e.g. urn:crewup:problem:unauthenticated")
        URI type,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int status,
        String detail,
        URI instance,

        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description = "The request's trace ID; shown to users as the error's reference")
        String traceId) {}
