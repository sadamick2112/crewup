package app.crewup.identity.internal;

import java.util.stream.Stream;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

/** The profile values CrewUp reads from an access token. */
record UserClaims(String subject, String email, String displayName) {

    /** Shown when the token has no name claims at all; the next token with a name replaces it. */
    static final String PLACEHOLDER_DISPLAY_NAME = "CrewUp user";

    static UserClaims from(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        String displayName = Stream.of(jwt.getClaimAsString("name"), jwt.getClaimAsString("preferred_username"), email)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(PLACEHOLDER_DISPLAY_NAME);
        return new UserClaims(jwt.getSubject(), email, displayName);
    }
}
