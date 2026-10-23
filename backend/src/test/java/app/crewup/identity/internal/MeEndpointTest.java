package app.crewup.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import app.crewup.IntegrationTest;
import app.crewup.shared.Ids;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

// Token validation is covered by ApiSecurityTest; here jwt() supplies an already-validated token.
@IntegrationTest
class MeEndpointTest {

    private final MockMvcTester mvc;
    private final JdbcTemplate jdbc;
    private final String subject = "test-subject-" + Ids.newId();

    @Autowired
    MeEndpointTest(MockMvcTester mvc, JdbcTemplate jdbc) {
        this.mvc = mvc;
        this.jdbc = jdbc;
    }

    @AfterEach
    void deleteTestUser() {
        jdbc.update("DELETE FROM users WHERE subject = ?", subject);
    }

    @Test
    void M0_6_firstCallProvisionsUser() {
        var response = getMe(Map.of("name", "Ada Lovelace", "email", "ada@example.test"));

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response).bodyJson().extractingPath("$.displayName").isEqualTo("Ada Lovelace");
        assertThat(response).bodyJson().extractingPath("$.email").isEqualTo("ada@example.test");
        assertThat(UUID.fromString(idOf(response)).version()).isEqualTo(7);
        assertThat(usersWithTestSubject()).isEqualTo(1);
    }

    @Test
    void M0_6_laterCallsReturnSameUser() {
        var claims = Map.<String, Object>of("name", "Ada Lovelace", "email", "ada@example.test");
        var first = getMe(claims);

        var second = getMe(claims);

        assertThat(second).hasStatus(HttpStatus.OK);
        assertThat(idOf(second)).isEqualTo(idOf(first));
        assertThat(usersWithTestSubject()).isEqualTo(1);
    }

    @Test
    void M0_6_profileRefreshesChangedClaims() {
        var first = getMe(Map.of("name", "Ada Lovelace", "email", "ada@example.test"));

        var second = getMe(Map.of("name", "Ada King", "email", "countess@example.test"));

        assertThat(second).hasStatus(HttpStatus.OK);
        assertThat(idOf(second)).isEqualTo(idOf(first));
        assertThat(second).bodyJson().extractingPath("$.displayName").isEqualTo("Ada King");
        assertThat(second).bodyJson().extractingPath("$.email").isEqualTo("countess@example.test");
    }

    @Test
    void M0_6_concurrentFirstCallsCreateOneUser() throws Exception {
        int callers = 10;
        var start = new CountDownLatch(1);
        List<Future<MvcTestResult>> calls = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(callers)) {
            for (int i = 0; i < callers; i++) {
                Callable<MvcTestResult> call = () -> {
                    start.await();
                    return getMe(Map.of("name", "Ada Lovelace"));
                };
                calls.add(pool.submit(call));
            }

            start.countDown(); // release all callers at once, so their first calls overlap

            var ids = new ArrayList<String>();
            for (var call : calls) {
                var response = call.get(30, TimeUnit.SECONDS);
                assertThat(response).hasStatus(HttpStatus.OK);
                ids.add(idOf(response));
            }
            assertThat(ids).containsOnly(ids.getFirst());
        }
        assertThat(usersWithTestSubject()).isEqualTo(1);
    }

    private MvcTestResult getMe(Map<String, Object> claims) {
        return mvc.get()
                .uri("/api/me")
                .with(jwt().jwt(token -> token.subject(subject).claims(all -> all.putAll(claims))))
                .exchange();
    }

    private static String idOf(MvcTestResult response) {
        try {
            return JsonPath.read(response.getResponse().getContentAsString(), "$.id");
        } catch (Exception e) {
            throw new AssertionError("Response has no readable id", e);
        }
    }

    private int usersWithTestSubject() {
        return jdbc.queryForObject("SELECT count(*) FROM users WHERE subject = ?", Integer.class, subject);
    }
}
