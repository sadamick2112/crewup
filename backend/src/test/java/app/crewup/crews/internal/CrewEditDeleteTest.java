package app.crewup.crews.internal;

import static org.assertj.core.api.Assertions.assertThat;

import app.crewup.IntegrationTest;
import app.crewup.crews.internal.CrewFixtures.TestUser;
import app.crewup.shared.Ids;
import app.crewup.shared.JsonLogLines;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

// Editing (M1-5) and deleting (M1-6) a crew: Administrators only (M1-16), with history and log lines (M1-17).
@IntegrationTest
@ExtendWith(OutputCaptureExtension.class)
class CrewEditDeleteTest {

    private static final String CHANGES = """
            {
              "name": " Thursday Gamers ",
              "establishedMonth": "2004-03",
              "iconKey": "PIZZA",
              "timeZone": "Europe/Berlin",
              "motto": " Roll for it ",
              "description": "We meet on Thursdays."
            }
            """;

    private final MockMvcTester mvc;
    private final JdbcTemplate jdbc;
    private final CrewFixtures fixtures;

    @Autowired
    CrewEditDeleteTest(MockMvcTester mvc, JdbcTemplate jdbc) {
        this.mvc = mvc;
        this.jdbc = jdbc;
        this.fixtures = new CrewFixtures(jdbc);
    }

    @AfterEach
    void cleanUp() {
        fixtures.cleanUp();
    }

    @Test
    void M1_5_GRP2_administratorUpdatesCrew() {
        TestUser admin = fixtures.user();
        UUID crewId = fixtures.crew("Alpha");
        fixtures.member(crewId, admin, MemberRole.ADMIN);

        var response = update(admin, crewId, "\"0\"", CHANGES);

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response).hasHeader(HttpHeaders.ETAG, "\"1\"");
        assertThat(response).bodyJson().extractingPath("$.name").isEqualTo("Thursday Gamers");
        assertThat(response).bodyJson().extractingPath("$.establishedMonth").isEqualTo("2004-03");
        assertThat(response).bodyJson().extractingPath("$.iconKey").isEqualTo("PIZZA");
        assertThat(response).bodyJson().extractingPath("$.timeZone").isEqualTo("Europe/Berlin");
        assertThat(response).bodyJson().extractingPath("$.motto").isEqualTo("Roll for it");
        assertThat(response).bodyJson().extractingPath("$.description").isEqualTo("We meet on Thursdays.");
        assertThat(response).bodyJson().extractingPath("$.myRole").isEqualTo("ADMIN");

        var fetched = get(admin, crewId);
        assertThat(fetched).hasHeader(HttpHeaders.ETAG, "\"1\"");
        assertThat(fetched).bodyJson().extractingPath("$.name").isEqualTo("Thursday Gamers");
    }

    @Test
    void M1_5_GRP2_emptiedOptionalFieldsBecomeNull() {
        TestUser admin = fixtures.user();
        UUID crewId = fixtures.crew("Alpha");
        fixtures.member(crewId, admin, MemberRole.ADMIN);
        update(admin, crewId, "\"0\"", CHANGES);

        var response = update(admin, crewId, "\"1\"", """
                {
                  "name": "Thursday Gamers",
                  "establishedMonth": "2004-03",
                  "iconKey": "PIZZA",
                  "timeZone": "Europe/Berlin",
                  "motto": " ",
                  "description": null
                }
                """);

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response).bodyJson().extractingPath("$.motto").isNull();
        assertThat(response).bodyJson().extractingPath("$.description").isNull();
    }

    @Test
    void M1_5_GRP2_invalidFieldsReturn400WithFieldErrors() {
        TestUser admin = fixtures.user();
        UUID crewId = fixtures.crew("Alpha");
        fixtures.member(crewId, admin, MemberRole.ADMIN);

        var response = update(admin, crewId, "\"0\"", """
                {
                  "name": " ",
                  "establishedMonth": "1899-12",
                  "iconKey": null,
                  "timeZone": "Mars/Olympus",
                  "motto": "%s"
                }
                """.formatted("x".repeat(151)));

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(response).bodyJson().extractingPath("$.type").isEqualTo("urn:crewup:problem:invalid-request");
        assertThat(response)
                .bodyJson()
                .extractingPath("$.errors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("name", "establishedMonth", "iconKey", "timeZone", "motto");
        assertThat(crewRow(crewId)).containsEntry("name", "Alpha");
    }

    @Test
    void M1_5_GRP2_staleIfMatchReturns412() {
        TestUser admin = fixtures.user();
        UUID crewId = fixtures.crew("Alpha");
        fixtures.member(crewId, admin, MemberRole.ADMIN);
        update(admin, crewId, "\"0\"", CHANGES);

        var response = update(admin, crewId, "\"0\"", CHANGES.replace("Thursday Gamers", "Stale Name"));

        assertThat(response).hasStatus(HttpStatus.PRECONDITION_FAILED);
        assertThat(response).bodyJson().extractingPath("$.type").isEqualTo("urn:crewup:problem:precondition-failed");
        assertThat(crewRow(crewId)).containsEntry("name", "Thursday Gamers");
    }

    @Test
    void M1_5_GRP2_missingIfMatchReturns428() {
        TestUser admin = fixtures.user();
        UUID crewId = fixtures.crew("Alpha");
        fixtures.member(crewId, admin, MemberRole.ADMIN);

        var response = update(admin, crewId, null, CHANGES);

        assertThat(response).hasStatus(HttpStatus.PRECONDITION_REQUIRED);
        assertThat(response).bodyJson().extractingPath("$.type").isEqualTo("urn:crewup:problem:precondition-required");
        assertThat(crewRow(crewId)).containsEntry("name", "Alpha");
    }

    @ParameterizedTest
    @EnumSource(
            value = MemberRole.class,
            names = {"FRIEND", "NEW_GUY"})
    void M1_5_GRP2_nonAdministratorGets403(MemberRole role) {
        TestUser user = fixtures.user();
        UUID crewId = fixtures.crew("Alpha");
        fixtures.member(crewId, user, role);

        var response = update(user, crewId, "\"0\"", CHANGES);

        assertThat(response).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(crewRow(crewId)).containsEntry("name", "Alpha");
    }

    @Test
    void M1_6_GRP3_deleteHidesCrewFromEveryone() {
        TestUser admin = fixtures.user();
        TestUser friend = fixtures.user();
        UUID crewId = fixtures.crew("Alpha");
        fixtures.member(crewId, admin, MemberRole.ADMIN);
        fixtures.member(crewId, friend, MemberRole.FRIEND);

        var response = delete(admin, crewId, " Alpha ");

        assertThat(response).hasStatus(HttpStatus.NO_CONTENT);
        for (TestUser user : List.of(admin, friend)) {
            assertThat(get(user, crewId)).hasStatus(HttpStatus.NOT_FOUND);
            var list = mvc.get().uri("/api/crews").with(user.token()).exchange();
            assertThat(list).bodyJson().extractingPath("$.items").asArray().isEmpty();
        }
        Map<String, Object> row = crewRow(crewId);
        assertThat(row.get("deleted_at")).isNotNull();
        assertThat(row).containsEntry("updated_by", admin.id());
    }

    @Test
    void M1_6_GRP3_wrongConfirmationDeletesNothing() {
        TestUser admin = fixtures.user();
        UUID crewId = fixtures.crew("Alpha");
        fixtures.member(crewId, admin, MemberRole.ADMIN);

        var response = delete(admin, crewId, "alpha");

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.type").isEqualTo("urn:crewup:problem:confirmation-mismatch");
        assertThat(crewRow(crewId).get("deleted_at")).isNull();
        assertThat(get(admin, crewId)).hasStatus(HttpStatus.OK);
    }

    @ParameterizedTest
    @EnumSource(
            value = MemberRole.class,
            names = {"FRIEND", "NEW_GUY"})
    void M1_6_GRP3_nonAdministratorGets403(MemberRole role) {
        TestUser user = fixtures.user();
        UUID crewId = fixtures.crew("Alpha");
        fixtures.member(crewId, user, role);

        var response = delete(user, crewId, "Alpha");

        assertThat(response).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(crewRow(crewId).get("deleted_at")).isNull();
    }

    @Test
    void M1_16_nonMemberGets404OnEditAndDelete() {
        TestUser stranger = fixtures.user();
        UUID crewId = fixtures.crew("Alpha");
        fixtures.member(crewId, fixtures.user(), MemberRole.ADMIN);

        for (UUID id : List.of(crewId, Ids.newId())) {
            assertNotFound(update(stranger, id, "\"0\"", CHANGES));
            assertNotFound(delete(stranger, id, "Alpha"));
        }
        assertThat(crewRow(crewId)).containsEntry("name", "Alpha");
        assertThat(crewRow(crewId).get("deleted_at")).isNull();
    }

    @Test
    void M1_16_deletedCrewReturns404OnEditAndDelete() {
        TestUser admin = fixtures.user();
        UUID crewId = fixtures.crew("Alpha");
        fixtures.member(crewId, admin, MemberRole.ADMIN);
        fixtures.softDelete(crewId);

        assertNotFound(update(admin, crewId, "\"0\"", CHANGES));
        assertNotFound(delete(admin, crewId, "Alpha"));
    }

    @Test
    void M1_17_editRecordsAuthor() {
        TestUser admin = fixtures.user();
        UUID crewId = fixtures.crew("Alpha");
        fixtures.member(crewId, admin, MemberRole.ADMIN);
        Map<String, Object> before = crewRow(crewId);

        update(admin, crewId, "\"0\"", CHANGES);

        Map<String, Object> after = crewRow(crewId);
        assertThat(after).containsEntry("updated_by", admin.id());
        assertThat(after.get("updated_by")).isNotEqualTo(after.get("created_by"));
        assertThat(after.get("updated_at")).isNotEqualTo(before.get("updated_at"));
        assertThat(after).containsEntry("created_at", before.get("created_at"));
    }

    @Test
    void M1_17_editAndDeleteAreLoggedWithIdsOnly(CapturedOutput output) {
        TestUser admin = fixtures.user();
        UUID crewId = fixtures.crew("Alpha");
        fixtures.member(crewId, admin, MemberRole.ADMIN);

        update(admin, crewId, "\"0\"", CHANGES);
        delete(admin, crewId, "Thursday Gamers");

        for (String message : List.of("Crew updated", "Crew deleted")) {
            var lines = JsonLogLines.events(output).stream()
                    .filter(event -> message.equals(JsonLogLines.text(event, "message")))
                    .filter(event -> crewId.toString().equals(JsonLogLines.text(event, "crewId")))
                    .toList();
            assertThat(lines).as(message).hasSize(1);
            assertThat(lines.getFirst().toString()).doesNotContain("Alpha", "Thursday", "Roll for it");
        }
    }

    private MvcTestResult update(TestUser user, UUID crewId, String ifMatch, String json) {
        var request = mvc.put()
                .uri("/api/crews/{crewId}", crewId)
                .with(user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
        if (ifMatch != null) {
            request.header(HttpHeaders.IF_MATCH, ifMatch);
        }
        return request.exchange();
    }

    private MvcTestResult delete(TestUser user, UUID crewId, String confirmName) {
        return mvc.delete()
                .uri("/api/crews/{crewId}", crewId)
                .param("confirmName", confirmName)
                .with(user.token())
                .exchange();
    }

    private MvcTestResult get(TestUser user, UUID crewId) {
        return mvc.get().uri("/api/crews/{crewId}", crewId).with(user.token()).exchange();
    }

    private Map<String, Object> crewRow(UUID crewId) {
        return jdbc.queryForMap(
                "SELECT name, created_at, created_by, updated_at, updated_by, deleted_at FROM crews WHERE id = ?",
                crewId);
    }

    private static void assertNotFound(MvcTestResult response) {
        assertThat(response).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(response).bodyJson().extractingPath("$.type").isEqualTo("urn:crewup:problem:not-found");
    }
}
