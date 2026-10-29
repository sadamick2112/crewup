package app.crewup.shared;

import org.springframework.http.HttpStatus;

/** The resource doesn't exist, or the caller may not know that it does (API-5). Modules subclass it per resource. */
public class NotFoundException extends ApiProblemException {

    public NotFoundException(String detail) {
        super(HttpStatus.NOT_FOUND, "urn:crewup:problem:not-found", detail);
    }
}
