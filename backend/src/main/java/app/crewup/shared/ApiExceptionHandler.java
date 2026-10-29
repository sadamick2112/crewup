package app.crewup.shared;

import java.net.URI;
import java.util.List;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * The one place errors become Problem Details (JAVA-11, API-4). Extends Spring's handler, so its built-in errors (such
 * as an unknown path's 404) keep their Problem Details, and Spring Boot's default handler steps aside. Validation
 * errors get an {@code errors} array naming each invalid field, so a form can show each message on its field (M1-22).
 * The {@code traceId} is added afterwards by {@code ProblemDetailsTraceIdAdvice}.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InvalidRequestException.class)
    ProblemDetail invalidRequest(InvalidRequestException e) {
        return invalidFields(List.of(e.invalidField()));
    }

    @ExceptionHandler(ApiProblemException.class)
    ProblemDetail apiProblem(ApiProblemException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(e.status(), e.getMessage());
        problem.setType(e.type());
        return problem;
    }

    /** Two saves of the same version at the same moment: the second fails at flush, after its If-Match check. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail concurrentUpdate(OptimisticLockingFailureException e) {
        return apiProblem(new PreconditionFailedException());
    }

    /** {@code @Valid} on a request body. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<InvalidField> fields = ex.getBindingResult().getAllErrors().stream()
                .map(ApiExceptionHandler::toInvalidField)
                .toList();
        return handleExceptionInternal(ex, invalidFields(fields), headers, status, request);
    }

    /** Constraints on handler parameters, such as {@code @Max} on a query parameter. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<InvalidField> fields = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new InvalidField(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        return handleExceptionInternal(ex, invalidFields(fields), headers, status, request);
    }

    private static InvalidField toInvalidField(ObjectError error) {
        // A class-level constraint has no field; the form shows it above the Save button.
        String field = error instanceof FieldError fieldError ? fieldError.getField() : "";
        return new InvalidField(field, error.getDefaultMessage());
    }

    private static ProblemDetail invalidFields(List<InvalidField> fields) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "The request has invalid fields");
        problem.setType(URI.create(InvalidRequestException.TYPE));
        problem.setProperty("errors", fields);
        return problem;
    }
}
