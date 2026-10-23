package app.crewup;

import app.crewup.shared.ProblemDetailsSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;

/**
 * Writes RFC 9457 Problem Details from security filters, which run before Spring MVC and so can't use its error
 * handling (API-4).
 */
@Component
class ProblemDetailsWriter {

    private final JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter();
    private final ProblemDetailsSupport problemDetails;

    ProblemDetailsWriter(ProblemDetailsSupport problemDetails) {
        this.problemDetails = problemDetails;
    }

    void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String type, String detail)
            throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(type));
        problem.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(status.value());
        converter.write(
                problemDetails.withTraceId(problem),
                MediaType.APPLICATION_PROBLEM_JSON,
                new ServletServerHttpResponse(response));
    }
}
