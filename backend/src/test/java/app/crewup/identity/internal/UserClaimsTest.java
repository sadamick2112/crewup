package app.crewup.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class UserClaimsTest {

    @Test
    void M0_6_displayNameIsTheNameClaim() {
        var jwt = token(Map.of("name", "Ada Lovelace", "preferred_username", "ada", "email", "ada@example.test"));

        assertThat(UserClaims.from(jwt).displayName()).isEqualTo("Ada Lovelace");
    }

    @Test
    void M0_6_displayNameFallsBackToPreferredUsername() {
        var jwt = token(Map.of("preferred_username", "ada", "email", "ada@example.test"));

        assertThat(UserClaims.from(jwt).displayName()).isEqualTo("ada");
    }

    @Test
    void M0_6_displayNameFallsBackToEmail() {
        var jwt = token(Map.of("email", "ada@example.test"));

        assertThat(UserClaims.from(jwt).displayName()).isEqualTo("ada@example.test");
    }

    @Test
    void M0_6_displayNameFallsBackToPlaceholderWithoutNameClaims() {
        var jwt = token(Map.of());

        assertThat(UserClaims.from(jwt).displayName()).isEqualTo("CrewUp user");
    }

    @Test
    void M0_6_blankNameClaimIsIgnored() {
        var jwt = token(Map.of("name", "  ", "preferred_username", "ada"));

        assertThat(UserClaims.from(jwt).displayName()).isEqualTo("ada");
    }

    @Test
    void M0_6_subjectAndEmailComeFromTheToken() {
        var claims = UserClaims.from(token(Map.of("email", "ada@example.test")));

        assertThat(claims.subject()).isEqualTo("subject-1");
        assertThat(claims.email()).isEqualTo("ada@example.test");
    }

    private static Jwt token(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("subject-1")
                .claims(all -> all.putAll(claims))
                .build();
    }
}
