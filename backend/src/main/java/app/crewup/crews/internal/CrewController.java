package app.crewup.crews.internal;

import app.crewup.identity.Users;
import app.crewup.shared.CursorPage;
import app.crewup.shared.Cursors;
import app.crewup.shared.ETags;
import app.crewup.shared.ProblemDetailSchema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

    private final CrewService crews;
    private final Users users;
    private final CallerMemberships memberships;

    CrewController(CrewService crews, Users users, CallerMemberships memberships) {
        this.crews = crews;
        this.users = users;
        this.memberships = memberships;
    }

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
        return crews.myCrews(users.currentUserId(jwt), cursor, limit);
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
        Versioned<CrewDetails> created = crews.create(users.currentUserId(jwt), request);
        return ResponseEntity.created(URI.create("/api/crews/" + created.value().id()))
                .eTag(ETags.of(created.version()))
                .body(created.value());
    }

    @GetMapping("/{crewId}")
    @PreAuthorize("@crewAccess.isMember(#crewId)")
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
        Versioned<CrewDetails> crew = crews.crew(crewId, memberships.require(crewId));
        return ResponseEntity.ok().eTag(ETags.of(crew.version())).body(crew.value());
    }

    @PutMapping(path = "/{crewId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@crewAccess.isAdministrator(#crewId)")
    @Operation(summary = "Update a crew", description = "Administrators only. Replaces every field.")
    @ApiResponse(
            responseCode = "200",
            description = "The updated crew",
            headers = @Header(name = "ETag", description = "Send back in If-Match to update it again"))
    @ApiResponse(
            responseCode = "400",
            description = "Invalid fields",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetailSchema.class)))
    @ApiResponse(
            responseCode = "403",
            description = "The caller isn't an Administrator of this crew",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetailSchema.class)))
    @ApiResponse(
            responseCode = "404",
            description = "No such crew, or the caller isn't a member",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetailSchema.class)))
    @ApiResponse(
            responseCode = "412",
            description = "Someone else changed the crew since the If-Match ETag was read",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetailSchema.class)))
    @ApiResponse(
            responseCode = "428",
            description = "If-Match is missing",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetailSchema.class)))
    ResponseEntity<CrewDetails> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID crewId,
            // Not required for Spring, so a missing header reaches ETags and gets 428 instead of 400.
            @Parameter(required = true, description = "The ETag from the last read of this crew")
                    @RequestHeader(name = HttpHeaders.IF_MATCH, required = false)
                    String ifMatch,
            @Valid @RequestBody UpdateCrewRequest request) {
        Versioned<CrewDetails> updated =
                crews.update(crewId, memberships.require(crewId), users.currentUserId(jwt), ifMatch, request);
        return ResponseEntity.ok().eTag(ETags.of(updated.version())).body(updated.value());
    }

    @DeleteMapping("/{crewId}")
    @PreAuthorize("@crewAccess.isAdministrator(#crewId)")
    @Operation(
            summary = "Delete a crew",
            description = "Administrators only. Hidden at once, erased after 30 days. The name confirms the intent.")
    @ApiResponse(responseCode = "204", description = "The crew is deleted")
    @ApiResponse(
            responseCode = "400",
            description = "The confirmation doesn't match the crew's name",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetailSchema.class)))
    @ApiResponse(
            responseCode = "403",
            description = "The caller isn't an Administrator of this crew",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetailSchema.class)))
    @ApiResponse(
            responseCode = "404",
            description = "No such crew, or the caller isn't a member",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetailSchema.class)))
    ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID crewId,
            @Parameter(description = "The crew's current name, to confirm") @RequestParam String confirmName) {
        crews.delete(crewId, memberships.require(crewId), users.currentUserId(jwt), confirmName);
        return ResponseEntity.noContent().build();
    }
}
