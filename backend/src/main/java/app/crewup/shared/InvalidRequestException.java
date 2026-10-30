package app.crewup.shared;

import org.springframework.http.HttpStatus;

/**
 * One request field is invalid in a way Bean Validation can't check, such as an unreadable cursor. Reported like a
 * validation error: a {@code 400} whose {@code errors} array names the field.
 */
public class InvalidRequestException extends ApiProblemException {

    static final String TYPE = "urn:crewup:problem:invalid-request";

    private final InvalidField invalidField;

    public InvalidRequestException(String field, String message) {
        super(HttpStatus.BAD_REQUEST, TYPE, "The request has invalid fields");
        this.invalidField = new InvalidField(field, message);
    }

    public InvalidField invalidField() {
        return invalidField;
    }
}
