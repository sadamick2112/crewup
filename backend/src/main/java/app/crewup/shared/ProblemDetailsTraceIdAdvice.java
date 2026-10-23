package app.crewup.shared;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Adds the {@code traceId} to every Problem Details body Spring MVC writes, including its built-in errors such as 404.
 * Typed as {@code Object} because those errors arrive as {@code ResponseEntity<Object>} with a {@link ProblemDetail}
 * body, which a {@code ResponseBodyAdvice<ProblemDetail>} wouldn't be asked about.
 */
@ControllerAdvice
class ProblemDetailsTraceIdAdvice implements ResponseBodyAdvice<Object> {

    private final ProblemDetailsSupport problemDetails;

    ProblemDetailsTraceIdAdvice(ProblemDetailsSupport problemDetails) {
        this.problemDetails = problemDetails;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        return body instanceof ProblemDetail problem ? problemDetails.withTraceId(problem) : body;
    }
}
