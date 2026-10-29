package app.crewup.crews.internal;

import static org.assertj.core.api.Assertions.assertThat;

import app.crewup.IntegrationTest;
import app.crewup.crews.internal.CrewFixtures.TestUser;
import app.crewup.shared.Ids;
import com.jayway.jsonpath.JsonPath;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

// Crew access checks (M1-16): a non-member can't tell a crew exists, and every crew endpoint is guarded.
@IntegrationTest
class CrewAccessTest {

    private final MockMvcTester mvc;
    private final RequestMappingHandlerMapping handlerMapping;
    private final CrewFixtures fixtures;

    @Autowired
    CrewAccessTest(
            MockMvcTester mvc,
            JdbcTemplate jdbc,
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        this.mvc = mvc;
        this.handlerMapping = handlerMapping;
        this.fixtures = new CrewFixtures(jdbc);
    }

    @AfterEach
    void cleanUp() {
        fixtures.cleanUp();
    }

    @Test
    void M1_16_nonMemberGets404LikeUnknownCrew() {
        TestUser stranger = fixtures.user();
        UUID crewId = fixtures.crew("Alpha");
        fixtures.member(crewId, fixtures.user(), MemberRole.ADMIN);

        var existing = mvc.get()
                .uri("/api/crews/{crewId}", crewId)
                .with(stranger.token())
                .exchange();
        var unknown = mvc.get()
                .uri("/api/crews/{crewId}", Ids.newId())
                .with(stranger.token())
                .exchange();

        for (var response : List.of(existing, unknown)) {
            assertThat(response).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(response).bodyJson().extractingPath("$.type").isEqualTo("urn:crewup:problem:not-found");
        }
        assertThat(existing).bodyJson().extractingPath("$.title").isEqualTo(textOf(unknown, "$.title"));
        assertThat(existing).bodyJson().extractingPath("$.detail").isEqualTo(textOf(unknown, "$.detail"));
    }

    @Test
    void M1_16_deletedCrewReturns404() {
        TestUser member = fixtures.user();
        UUID crewId = fixtures.crew("Alpha");
        fixtures.member(crewId, member, MemberRole.ADMIN);
        fixtures.softDelete(crewId);

        var response = mvc.get()
                .uri("/api/crews/{crewId}", crewId)
                .with(member.token())
                .exchange();

        assertThat(response).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(response).bodyJson().extractingPath("$.type").isEqualTo("urn:crewup:problem:not-found");
    }

    // A forgotten @PreAuthorize fails open, so check every handler under a crew's path (M2 extends this to the
    // guard test of architecture §6).
    @Test
    void M1_16_everyCrewEndpointHasPreAuthorize() {
        List<HandlerMethod> crewHandlers = handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getKey().getPatternValues().stream()
                        .anyMatch(pattern -> pattern.startsWith("/api/crews/{crewId}")))
                .map(entry -> entry.getValue())
                .toList();

        assertThat(crewHandlers).isNotEmpty();
        assertThat(crewHandlers)
                .allSatisfy(handler -> assertThat(
                                AnnotatedElementUtils.hasAnnotation(handler.getMethod(), PreAuthorize.class))
                        .as("@PreAuthorize on %s", handler)
                        .isTrue());
    }

    private static String textOf(MvcTestResult response, String path) {
        try {
            return JsonPath.read(response.getResponse().getContentAsString(), path);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
