package app.crewup;

import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A test-only {@code /api} endpoint, so security tests don't depend on a real feature endpoint. It returns the token's
 * subject, which proves the request was authenticated with that token. Hidden from OpenAPI, so it never reaches the
 * committed contract.
 */
@Hidden
@TestComponent
@RestController
class ApiProbeController {

    static final String PATH = "/api/test-probe";

    @GetMapping(PATH)
    Map<String, String> probe(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("subject", jwt.getSubject());
    }
}
