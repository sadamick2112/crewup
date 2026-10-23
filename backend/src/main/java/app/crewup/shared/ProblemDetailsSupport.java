package app.crewup.shared;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/**
 * Adds the current request's {@code traceId} to Problem Details error responses (M0-27, LOG-6), so a user can quote
 * the ID shown with an error and it matches the backend's log lines.
 */
@Component
public class ProblemDetailsSupport {

    private final Tracer tracer;

    ProblemDetailsSupport(Tracer tracer) {
        this.tracer = tracer;
    }

    public ProblemDetail withTraceId(ProblemDetail problem) {
        Span span = tracer.currentSpan();
        if (span != null) {
            problem.setProperty("traceId", span.context().traceId());
        }
        return problem;
    }
}
