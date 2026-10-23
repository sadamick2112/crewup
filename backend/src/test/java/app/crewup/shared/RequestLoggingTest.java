package app.crewup.shared;

import static app.crewup.shared.JsonLogLines.field;
import static app.crewup.shared.JsonLogLines.requestLines;
import static app.crewup.shared.JsonLogLines.requestLinesForTrace;
import static app.crewup.shared.JsonLogLines.text;
import static org.assertj.core.api.Assertions.assertThat;

import app.crewup.IntegrationTest;
import app.crewup.TestOidcProvider;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

// JSON logs (as outside local development) so tests read fields instead of matching text.
@IntegrationTest
@TestPropertySource(properties = "logging.structured.format.console=ecs")
@ExtendWith(OutputCaptureExtension.class)
class RequestLoggingTest {

    private static final String PROBE_PATH = "/api/test-probe";

    private final MockMvcTester mvc;
    private final JdbcTemplate jdbc;
    private final TestOidcProvider oidc = TestOidcProvider.instance();

    @Autowired
    RequestLoggingTest(MockMvcTester mvc, JdbcTemplate jdbc) {
        this.mvc = mvc;
        this.jdbc = jdbc;
    }

    @Test
    void M0_26_apiRequestIsLoggedOnceWithRouteStatusAndDuration(CapturedOutput output) {
        var trace = TestTraceparent.random();

        get(PROBE_PATH, trace, oidc.validToken());

        var lines = requestLinesForTrace(output, trace.traceId());
        assertThat(lines).hasSize(1);
        var line = lines.getFirst();
        assertThat(text(line, "http.method")).isEqualTo("GET");
        assertThat(text(line, "http.route")).isEqualTo(PROBE_PATH);
        assertThat(text(line, "http.status")).isEqualTo("200");
        assertThat(Long.parseLong(text(line, "durationMs"))).isNotNegative();
        assertThat(text(line, "subject")).isEqualTo(TestOidcProvider.SUBJECT);
    }

    @Test
    void M0_26_rejectedApiRequestIsLoggedAsUnmatched(CapturedOutput output) {
        var trace = TestTraceparent.random();

        get(PROBE_PATH, trace, null);

        var lines = requestLinesForTrace(output, trace.traceId());
        assertThat(lines).hasSize(1);
        var line = lines.getFirst();
        assertThat(text(line, "http.route")).isEqualTo("UNMATCHED");
        assertThat(text(line, "http.status")).isEqualTo("401");
        assertThat(field(line, "subject")).isNull();
    }

    @Test
    void M0_26_requestLogContainsNoPersonalDataOrToken(CapturedOutput output) {
        var trace = TestTraceparent.random();
        var token = oidc.validTokenWithClaims(Map.of("name", "Ada Lovelace", "email", "ada@example.test"));

        try {
            get("/api/me", trace, token);
        } finally {
            jdbc.update("DELETE FROM users WHERE subject = ?", TestOidcProvider.SUBJECT);
        }

        assertThat(requestLinesForTrace(output, trace.traceId())).hasSize(1);
        assertThat(output.getOut())
                .doesNotContain("ada@example.test")
                .doesNotContain("Ada Lovelace")
                .doesNotContain(token);
    }

    @Test
    void M0_26_logsAreEcsJsonWhenStructuredFormatIsOn(CapturedOutput output) {
        var trace = TestTraceparent.random();

        get(PROBE_PATH, trace, oidc.validToken());

        var lines = requestLinesForTrace(output, trace.traceId());
        assertThat(lines).hasSize(1);
        var line = lines.getFirst();
        assertThat(field(line, "@timestamp")).isNotNull();
        assertThat(text(line, "log.level")).isEqualTo("INFO");
        assertThat(text(line, "service.name")).isEqualTo("crewup");
        assertThat(field(line, "ecs.version")).isNotNull();
        assertThat(text(line, "message")).isNotBlank();
    }

    @Test
    void M0_27_incomingTraceparentIsContinued(CapturedOutput output) {
        var trace = TestTraceparent.random();

        get(PROBE_PATH, trace, oidc.validToken());

        var lines = requestLinesForTrace(output, trace.traceId());
        assertThat(lines).hasSize(1);
        // Same trace, but the backend's own span: a child of the caller's span.
        assertThat(text(lines.getFirst(), "spanId")).isNotEqualTo(trace.parentSpanId());
    }

    @Test
    void M0_27_newTraceStartsWithoutTraceparent(CapturedOutput output) {
        get(PROBE_PATH, null, oidc.validToken());
        get(PROBE_PATH, null, oidc.validToken());

        var traceIds =
                requestLines(output).stream().map(line -> text(line, "traceId")).toList();
        assertThat(traceIds)
                .hasSize(2)
                .doesNotHaveDuplicates()
                .allSatisfy(
                        traceId -> assertThat(traceId).matches("[0-9a-f]{32}").isNotEqualTo("0".repeat(32)));
    }

    private void get(String path, TestTraceparent trace, String token) {
        var request = mvc.get().uri(path);
        if (trace != null) {
            request.header("traceparent", trace.header());
        }
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        request.exchange();
    }
}
