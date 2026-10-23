package app.crewup;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@IntegrationTest
class ApiSecurityTest {

    private final MockMvcTester mvc;
    private final TestOidcProvider oidc = TestOidcProvider.instance();

    @Autowired
    ApiSecurityTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    void M0_4_unauthenticatedApiCallReturns401WithProblemDetails() {
        var response = mvc.get().uri(ApiProbeController.PATH).exchange();

        assertUnauthenticatedProblem(response);
    }

    @Test
    void M0_4_tokenWithWrongAudienceIsRejected() {
        // Same signature, issuer and expiry as the accepted token in M0_4_validTokenIsAccepted; only aud differs.
        var token = oidc.token("some-other-client");

        var response = mvc.get()
                .uri(ApiProbeController.PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange();

        assertUnauthenticatedProblem(response);
    }

    @Test
    void M0_4_validTokenIsAccepted() {
        var response = mvc.get()
                .uri(ApiProbeController.PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + oidc.validToken())
                .exchange();

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response).bodyJson().extractingPath("$.subject").isEqualTo(TestOidcProvider.SUBJECT);
    }

    @Test
    void M0_4_deniedRequestReturns403WithProblemDetails() {
        var response = mvc.get()
                .uri("/outside-the-api")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + oidc.validToken())
                .exchange();

        assertThat(response).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.type").isEqualTo("urn:crewup:problem:access-denied");
        assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(403);
    }

    private static void assertUnauthenticatedProblem(MvcTestResult response) {
        assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.type").isEqualTo("urn:crewup:problem:unauthenticated");
        assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(401);
        // HTTP requires a 401 to name the authentication scheme (RFC 9110 §15.5.2, RFC 6750 §3).
        assertThat(response.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE))
                .startsWith("Bearer");
    }
}
