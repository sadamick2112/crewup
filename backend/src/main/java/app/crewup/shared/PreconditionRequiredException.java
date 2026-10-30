package app.crewup.shared;

import org.springframework.http.HttpStatus;

/** The update came without {@code If-Match}, so it could overwrite someone else's change unnoticed (API-7). */
public class PreconditionRequiredException extends ApiProblemException {

    public PreconditionRequiredException() {
        super(
                HttpStatus.PRECONDITION_REQUIRED,
                "urn:crewup:problem:precondition-required",
                "Send the If-Match header with the ETag from the last read");
    }
}
