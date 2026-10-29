package app.crewup.crews.internal;

import static org.assertj.core.api.Assertions.assertThat;

import app.crewup.IntegrationTest;
import app.crewup.crews.internal.CrewFixtures.TestUser;
import app.crewup.shared.JsonLogLines;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

// Creating, listing and viewing crews (M1-1…M1-4) and their records (M1-17).
@IntegrationTest
@ExtendWith(OutputCaptureExtension.class)
class CrewEndpointsTest {

    private static final String NEW_CREW = """
            {
              "name": " Sweinbrothar Boy's Night ",
              "establishedMonth": "1999-10",
              "iconKey": "DICE",
              "timeZone": "America/Chicago",
              "motto": "Brothers by choice",
              "description": null,
              "creatorFirstName": " Shawn "
            }
            """;

    private final MockMvcTester mvc;
    private final JdbcTemplate jdbc;
    private final CrewFixtures fixtures;

    @Autowired
    CrewEndpointsTest(MockMvcTester mvc, JdbcTemplate jdbc) {
        this.mvc = mvc;
        this.jdbc = jdbc;
        this.fixtures = new CrewFixtures(jdbc);
    }

    @AfterEach
    void cleanUp() {
        fixtures.cleanUp();
    }

    @Test
    void M1_1_GRP1_createsCrewAndReturnsLocation() throws Exception {
        TestUser user = fixtures.user();

        var response = create(user, NEW_CREW);

        assertThat(response).hasStatus(HttpStatus.CREATED);
        String location = response.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(location).matches("/api/crews/[0-9a-f-]{36}");
        assertThat(response.getResponse().getHeader(HttpHeaders.ETAG)).isNotBlank();
        assertThat(response).bodyJson().extractingPath("$.name").isEqualTo("Sweinbrothar Boy's Night");
        assertThat(response).bodyJson().extractingPath("$.establishedMonth").isEqualTo("1999-10");
        assertThat(response).bodyJson().extractingPath("$.iconKey").isEqualTo("DICE");
        assertThat(response).bodyJson().extractingPath("$.timeZone").isEqualTo("America/Chicago");
        assertThat(response).bodyJson().extractingPath("$.motto").isEqualTo("Brothers by choice");

        var fetched = mvc.get().uri(location).with(user.token()).exchange();
        assertThat(fetched).hasStatus(HttpStatus.OK);
        assertThat(fetched).bodyJson().extractingPath("$.name").isEqualTo("Sweinbrothar Boy's Night");
    }

    @Test
    void M1_1_GRP1_invalidFieldsReturn400WithFieldErrors() {
        TestUser user = fixtures.user();

        var response = create(user, """
                {
                  "name": "",
                  "establishedMonth": "2999-01",
                  "iconKey": null,
                  "timeZone": "+02:00",
                  "creatorFirstName": " "
                }
                """);

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(response).bodyJson().extractingPath("$.type").isEqualTo("urn:crewup:problem:invalid-request");
        assertThat(response)
                .bodyJson()
                .extractingPath("$.errors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("name", "establishedMonth", "iconKey", "timeZone", "creatorFirstName");
        assertThat(crewsOf(user)).isZero();
    }

    @Test
    void M1_2_GRP1_creatorBecomesActiveAdministrator() throws Exception {
        TestUser user = fixtures.user();

        var response = create(user, NEW_CREW);

        assertThat(response).hasStatus(HttpStatus.CREATED);
        String body = response.getResponse().getContentAsString();
        UUID crewId = UUID.fromString(JsonPath.read(body, "$.id"));
        Map<String, Object> member =
                jdbc.queryForMap("SELECT id, user_id, role, status, first_name FROM members WHERE crew_id = ?", crewId);
        assertThat(member)
                .containsEntry("user_id", user.id())
                .containsEntry("role", "ADMIN")
                .containsEntry("status", "ACTIVE")
                .containsEntry("first_name", "Shawn");
        assertThat(response).bodyJson().extractingPath("$.myRole").isEqualTo("ADMIN");
        assertThat(response)
                .bodyJson()
                .extractingPath("$.myMemberId")
                .isEqualTo(member.get("id").toString());
    }

    @Test
    void M1_3_ACC2_listsOnlyCallersCrewsWithRole() {
        TestUser caller = fixtures.user();
        TestUser other = fixtures.user();
        fixtures.member(fixtures.crew("Alpha"), caller, MemberRole.ADMIN);
        fixtures.member(fixtures.crew("Beta"), caller, MemberRole.FRIEND);
        fixtures.member(fixtures.crew("Gamma"), other, MemberRole.ADMIN);

        var response = mvc.get().uri("/api/crews").with(caller.token()).exchange();

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response)
                .bodyJson()
                .extractingPath("$.items[*].name")
                .asArray()
                .containsExactly("Alpha", "Beta");
        assertThat(response)
                .bodyJson()
                .extractingPath("$.items[*].myRole")
                .asArray()
                .containsExactly("ADMIN", "FRIEND");
        assertThat(response).bodyJson().extractingPath("$.nextCursor").isNull();
    }

    @Test
    void M1_3_ACC2_leavesOutDeletedCrews() {
        TestUser caller = fixtures.user();
        fixtures.member(fixtures.crew("Alpha"), caller, MemberRole.ADMIN);
        UUID deleted = fixtures.crew("Beta");
        fixtures.member(deleted, caller, MemberRole.ADMIN);
        fixtures.softDelete(deleted);

        var response = mvc.get().uri("/api/crews").with(caller.token()).exchange();

        assertThat(response)
                .bodyJson()
                .extractingPath("$.items[*].name")
                .asArray()
                .containsExactly("Alpha");
    }

    @Test
    void M1_3_ACC2_pagesWithCursorInNameOrder() throws Exception {
        TestUser caller = fixtures.user();
        for (String name : new String[] {"beta", "Alpha", "gamma"}) {
            fixtures.member(fixtures.crew(name), caller, MemberRole.ADMIN);
        }

        var first = mvc.get()
                .uri("/api/crews")
                .param("limit", "2")
                .with(caller.token())
                .exchange();
        String cursor = JsonPath.read(first.getResponse().getContentAsString(), "$.nextCursor");
        var second = mvc.get()
                .uri("/api/crews")
                .param("limit", "2")
                .param("cursor", cursor)
                .with(caller.token())
                .exchange();

        assertThat(first).bodyJson().extractingPath("$.items[*].name").asArray().containsExactly("Alpha", "beta");
        assertThat(cursor).isNotBlank();
        assertThat(second)
                .bodyJson()
                .extractingPath("$.items[*].name")
                .asArray()
                .containsExactly("gamma");
        assertThat(second).bodyJson().extractingPath("$.nextCursor").isNull();
    }

    @Test
    void M1_4_memberGetsCrewWithOwnRoleAndETag() {
        TestUser caller = fixtures.user();
        UUID crewId = fixtures.crew("Alpha");
        UUID memberId = fixtures.member(crewId, caller, MemberRole.FRIEND);

        var response = mvc.get()
                .uri("/api/crews/{crewId}", crewId)
                .with(caller.token())
                .exchange();

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response).hasHeader(HttpHeaders.ETAG, "\"0\"");
        assertThat(response).bodyJson().extractingPath("$.id").isEqualTo(crewId.toString());
        assertThat(response).bodyJson().extractingPath("$.name").isEqualTo("Alpha");
        assertThat(response).bodyJson().extractingPath("$.establishedMonth").isEqualTo("1999-10");
        assertThat(response).bodyJson().extractingPath("$.iconKey").isEqualTo("USERS");
        assertThat(response).bodyJson().extractingPath("$.timeZone").isEqualTo("America/Chicago");
        assertThat(response).bodyJson().extractingPath("$.motto").isNull();
        assertThat(response).bodyJson().extractingPath("$.myMemberId").isEqualTo(memberId.toString());
        assertThat(response).bodyJson().extractingPath("$.myRole").isEqualTo("FRIEND");
    }

    @Test
    void M1_17_recordsAuthorsAndTimes() throws Exception {
        TestUser user = fixtures.user();
        Instant before = Instant.now();

        var response = create(user, NEW_CREW);

        Instant after = Instant.now();
        UUID crewId = UUID.fromString(JsonPath.read(response.getResponse().getContentAsString(), "$.id"));
        for (String sql : new String[] {
            "SELECT created_by, updated_by, created_at, updated_at FROM crews WHERE id = ?",
            "SELECT created_by, updated_by, created_at, updated_at FROM members WHERE crew_id = ?"
        }) {
            Map<String, Object> row = jdbc.queryForMap(sql, crewId);
            assertThat(row).containsEntry("created_by", user.id()).containsEntry("updated_by", user.id());
            assertThat(((java.sql.Timestamp) row.get("created_at")).toInstant()).isBetween(before, after);
            assertThat(row.get("updated_at")).isEqualTo(row.get("created_at"));
        }
    }

    @Test
    void M1_17_changesAreLoggedWithIdsOnly(CapturedOutput output) throws Exception {
        TestUser user = fixtures.user();

        var response = create(user, NEW_CREW);

        String crewId = JsonPath.read(response.getResponse().getContentAsString(), "$.id");
        var created = JsonLogLines.events(output).stream()
                .filter(event -> "Crew created".equals(JsonLogLines.text(event, "message")))
                .filter(event -> crewId.equals(JsonLogLines.text(event, "crewId")))
                .toList();
        assertThat(created).hasSize(1);
        assertThat(created.getFirst().toString()).doesNotContain("Sweinbrothar", "Shawn", "Brothers by choice");
    }

    private MvcTestResult create(TestUser user, String json) {
        return mvc.post()
                .uri("/api/crews")
                .with(user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private int crewsOf(TestUser user) {
        return jdbc.queryForObject("SELECT count(*) FROM members WHERE user_id = ?", Integer.class, user.id());
    }
}
