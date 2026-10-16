package app.crewup.identity.internal;

import app.crewup.shared.ProblemDetailSchema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Profile")
class MeController {

    private final UserProvisioningService provisioning;

    MeController(UserProvisioningService provisioning) {
        this.provisioning = provisioning;
    }

    @GetMapping(path = "/api/me", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get the signed-in user's profile",
            description = "The first call creates the user from the access token; later calls return the same user.")
    @ApiResponse(responseCode = "200", description = "The signed-in user's profile")
    @ApiResponse(
            responseCode = "401",
            description = "Missing or invalid access token",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetailSchema.class)))
    UserProfile me(@AuthenticationPrincipal Jwt jwt) {
        return UserProfile.from(provisioning.currentUser(jwt));
    }
}
