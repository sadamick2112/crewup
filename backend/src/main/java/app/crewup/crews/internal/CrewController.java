package app.crewup.crews.internal;

import app.crewup.shared.CursorPage;
import app.crewup.shared.Cursors;
import app.crewup.shared.ProblemDetailSchema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/crews", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Crews")
@ApiResponse(
        responseCode = "401",
        description = "Missing or invalid access token",
        content =
                @Content(
                        mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetailSchema.class)))
class CrewController {

    @GetMapping
    @Operation(summary = "List the caller's crews", description = "Sorted by name, one page at a time.")
    @ApiResponse(responseCode = "200", description = "One page of the caller's crews")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid cursor or limit",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetailSchema.class)))
    CursorPage<CrewSummary> myCrews(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "" + Cursors.DEFAULT_LIMIT) @Min(1) @Max(Cursors.MAX_LIMIT) int limit) {
        throw new UnsupportedOperationException("not implemented");
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a crew", description = "The caller becomes its first Member, an Administrator.")
    @ApiResponse(
            responseCode = "201",
            description = "The new crew",
            headers = {
                @Header(name = "Location", description = "The new crew's URL"),
                @Header(name = "ETag", description = "Send back in If-Match to update it")
            })
    @ApiResponse(
            responseCode = "400",
            description = "Invalid fields",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetailSchema.class)))
    ResponseEntity<CrewDetails> create(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateCrewRequest request) {
        throw new UnsupportedOperationException("not implemented");
    }

    @GetMapping("/{crewId}")
    @Operation(summary = "Get a crew", description = "With the caller's own Member id and role.")
    @ApiResponse(
            responseCode = "200",
            description = "The crew",
            headers = @Header(name = "ETag", description = "Send back in If-Match to update it"))
    @ApiResponse(
            responseCode = "404",
            description = "No such crew, or the caller isn't a member",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetailSchema.class)))
    ResponseEntity<CrewDetails> crew(@PathVariable UUID crewId) {
        throw new UnsupportedOperationException("not implemented");
    }
}
