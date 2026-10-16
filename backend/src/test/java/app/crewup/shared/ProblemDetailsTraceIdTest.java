package app.crewup.shared;

import static org.assertj.core.api.Assertions.assertThat;

import app.crewup.IntegrationTest;
import app.crewup.TestOidcProvider;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@IntegrationTest
class ProblemDetailsTraceIdTest {

    private final MockMvcTester mvc;

    @Autowired
    ProblemDetailsTraceIdTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    static Stream<Arguments> errors() {
        return Stream.of(
                Arguments.of("401 from security", "/api/test-probe", false, HttpStatus.UNAUTHORIZED),
                Arguments.of("403 from security", "/outside-the-api", true, HttpStatus.FORBIDDEN),
                Arguments.of("404 from Spring MVC", "/api/no-such-endpoint", true, HttpStatus.NOT_FOUND));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("errors")
    void M0_27_problemDetailsIncludeTraceId(String error, String path, boolean signedIn, HttpStatus status) {
        var trace = TestTraceparent.random();
        var request = mvc.get().uri(path).header("traceparent", trace.header());
        if (signedIn) {
            request.header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + TestOidcProvider.instance().validToken());
        }

        var response = request.exchange();

        assertThat(response).hasStatus(status);
        assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.traceId").isEqualTo(trace.traceId());
    }
}
