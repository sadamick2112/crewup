package app.crewup.shared;

import org.springframework.http.HttpStatus;

/** The update was based on an outdated version: someone else saved first (API-7). */
public class PreconditionFailedException extends ApiProblemException {

    public PreconditionFailedException() {
        super(
                HttpStatus.PRECONDITION_FAILED,
                "urn:crewup:problem:precondition-failed",
                "Someone else changed this since it was loaded");
    }
}
