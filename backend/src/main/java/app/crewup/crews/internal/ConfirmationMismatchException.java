package app.crewup.crews.internal;

import app.crewup.shared.ApiProblemException;
import org.springframework.http.HttpStatus;

/** The name sent to confirm a deletion isn't the crew's name, so nothing was deleted (M1-6). */
class ConfirmationMismatchException extends ApiProblemException {

    ConfirmationMismatchException() {
        super(
                HttpStatus.BAD_REQUEST,
                "urn:crewup:problem:confirmation-mismatch",
                "The confirmation doesn't match the crew's name");
    }
}
