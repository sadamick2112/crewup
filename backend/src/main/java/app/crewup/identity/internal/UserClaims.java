package app.crewup.identity.internal;

import org.springframework.security.oauth2.jwt.Jwt;

/** The profile values CrewUp reads from an access token. */
record UserClaims(String subject, String email, String displayName) {

    static UserClaims from(Jwt jwt) {
        throw new UnsupportedOperationException("not implemented");
    }
}
