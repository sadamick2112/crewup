package app.crewup;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** Answers a missing or invalid access token with 401 and Problem Details (M0-4). */
@Component
class ProblemDetailsAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemDetailsWriter writer;

    ProblemDetailsAuthenticationEntryPoint(ProblemDetailsWriter writer) {
        this.writer = writer;
    }

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        // RFC 6750 §3: name the scheme, and flag a rejected token without saying why (LOG-6).
        String challenge =
                authException instanceof InvalidBearerTokenException ? "Bearer error=\"invalid_token\"" : "Bearer";
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, challenge);
        writer.write(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "urn:crewup:problem:unauthenticated",
                "A valid access token is required.");
    }
}
