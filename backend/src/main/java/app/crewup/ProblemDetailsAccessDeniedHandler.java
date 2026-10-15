package app.crewup;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Answers a signed-in request that isn't allowed with 403 and Problem Details (API-4). */
@Component
class ProblemDetailsAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemDetailsWriter writer;

    ProblemDetailsAccessDeniedHandler(ProblemDetailsWriter writer) {
        this.writer = writer;
    }

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        writer.write(
                request,
                response,
                HttpStatus.FORBIDDEN,
                "urn:crewup:problem:access-denied",
                "You don't have access to this resource.");
    }
}
