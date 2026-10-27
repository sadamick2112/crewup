package app.crewup.identity.internal;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** The signed-in user's profile, as returned by {@code GET /api/me}. */
record UserProfile(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Name shown to other members")
        String displayName,

        @Schema(
                types = {"string", "null"},
                requiredMode = Schema.RequiredMode.REQUIRED,
                description = "Null if the identity provider has no email for the user")
        String email) {

    static UserProfile from(User user) {
        return new UserProfile(user.getId(), user.getDisplayName(), user.getEmail());
    }
}
