package app.crewup.identity;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** The identity module's API for other modules: who the signed-in caller is, as a CrewUp User. */
public interface Users {

    /**
     * The id of the caller's User, created on first use (M0-6), so a request can come before any {@code GET /api/me}.
     */
    UUID currentUserId(Jwt token);
}
