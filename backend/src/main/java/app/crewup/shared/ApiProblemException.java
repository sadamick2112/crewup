package app.crewup.shared;

import java.net.URI;
import org.springframework.http.HttpStatus;

/**
 * A domain error that the API reports as RFC 9457 Problem Details, with a stable type URI per kind of error (API-4).
 * Modules throw subclasses, and {@code ApiExceptionHandler} turns them into responses in one place (JAVA-11).
 */
public abstract class ApiProblemException extends RuntimeException {

    private final HttpStatus status;
    private final URI type;

    protected ApiProblemException(HttpStatus status, String type, String detail) {
        super(detail);
        this.status = status;
        this.type = URI.create(type);
    }

    public HttpStatus status() {
        return status;
    }

    public URI type() {
        return type;
    }
}
