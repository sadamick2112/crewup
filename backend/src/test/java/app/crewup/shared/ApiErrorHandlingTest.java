package app.crewup.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import app.crewup.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

// The shared API plumbing through ApiErrorProbeController: field errors (M1-1, M1-22), ETags (M1-5), not found
// (M1-16, M1-24) and cursor pages (M1-11), as every module's endpoints will use them.
@IntegrationTest
class ApiErrorHandlingTest {

    private static final String PATH = ApiErrorProbeController.PATH;

    private final MockMvcTester mvc;

    @Autowired
    ApiErrorHandlingTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    void M1_1_invalidRequestReturns400WithFieldPaths() {
        var response = mvc.post()
                .uri(PATH + "/validated")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": " ", "links": [{"url": ""}]}
                        """)
                .exchange();

        assertProblem(response, HttpStatus.BAD_REQUEST, "urn:crewup:problem:invalid-request");
        assertThat(response)
                .bodyJson()
                .extractingPath("$.errors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("name", "links[0].url");
        assertThat(response)
                .bodyJson()
                .extractingPath("$.errors[*].message")
                .asArray()
                .doesNotContain("", null);
    }

    @Test
    void M1_5_matchingIfMatchSucceedsWithNewETag() {
        var response = saveVersioned("\"3\"");

        assertThat(response).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(response).hasHeader(HttpHeaders.ETAG, "\"4\"");
    }

    @Test
    void M1_5_staleIfMatchReturns412() {
        var response = saveVersioned("\"2\"");

        assertProblem(response, HttpStatus.PRECONDITION_FAILED, "urn:crewup:problem:precondition-failed");
    }

    @Test
    void M1_5_missingIfMatchReturns428() {
        var response = saveVersioned(null);

        assertProblem(response, HttpStatus.PRECONDITION_REQUIRED, "urn:crewup:problem:precondition-required");
    }

    @Test
    void M1_5_concurrentSaveReturns412() {
        var response = mvc.get().uri(PATH + "/optimistic-lock").with(jwt()).exchange();

        assertProblem(response, HttpStatus.PRECONDITION_FAILED, "urn:crewup:problem:precondition-failed");
    }

    @Test
    void M1_16_notFoundReturns404WithTypeAndTraceId() {
        var response = mvc.get().uri(PATH + "/not-found").with(jwt()).exchange();

        assertProblem(response, HttpStatus.NOT_FOUND, "urn:crewup:problem:not-found");
        assertThat(response).bodyJson().extractingPath("$.detail").isEqualTo("No such thing");
    }

    @Test
    void M1_11_cursorPagesThroughAllItemsInOrder() throws Exception {
        List<String> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do {
            var request = mvc.get().uri(PATH + "/names").param("limit", "2").with(jwt());
            if (cursor != null) {
                request.param("cursor", cursor);
            }
            var response = request.exchange();
            assertThat(response).hasStatus(HttpStatus.OK);
            String body = response.getResponse().getContentAsString();
            seen.addAll(JsonPath.read(body, "$.items"));
            cursor = JsonPath.read(body, "$.nextCursor");
            pages++;
        } while (cursor != null && pages < 10);

        assertThat(seen).containsExactlyElementsOf(ApiErrorProbeController.NAMES);
        assertThat(pages).isEqualTo(3);
    }

    @Test
    void M1_11_tamperedCursorReturns400() {
        var response = mvc.get()
                .uri(PATH + "/names")
                .param("cursor", "not-a-cursor")
                .with(jwt())
                .exchange();

        assertProblem(response, HttpStatus.BAD_REQUEST, "urn:crewup:problem:invalid-request");
        assertThat(response).bodyJson().extractingPath("$.errors[0].field").isEqualTo("cursor");
    }

    @Test
    void M1_11_limitAbove100Returns400() {
        var response =
                mvc.get().uri(PATH + "/names").param("limit", "101").with(jwt()).exchange();

        assertProblem(response, HttpStatus.BAD_REQUEST, "urn:crewup:problem:invalid-request");
        assertThat(response).bodyJson().extractingPath("$.errors[0].field").isEqualTo("limit");
    }

    private MvcTestResult saveVersioned(String ifMatch) {
        var request = mvc.put().uri(PATH + "/versioned").with(jwt());
        if (ifMatch != null) {
            request.header(HttpHeaders.IF_MATCH, ifMatch);
        }
        return request.exchange();
    }

    private static void assertProblem(MvcTestResult response, HttpStatus status, String type) {
        assertThat(response).hasStatus(status);
        assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.type").isEqualTo(type);
        assertThat(response).bodyJson().extractingPath("$.traceId").asString().isNotBlank();
    }
}
