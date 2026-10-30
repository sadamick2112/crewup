package app.crewup.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import app.crewup.IntegrationTest;
import app.crewup.shared.Ids;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

// The identity module's API for other modules (plan §4.1): crews finds the caller's User through it (M1-2).
@IntegrationTest
class UsersTest {

    private final Users users;
    private final MockMvcTester mvc;
    private final JdbcTemplate jdbc;
    private final String subject = "test-subject-" + Ids.newId();

    @Autowired
    UsersTest(Users users, MockMvcTester mvc, JdbcTemplate jdbc) {
        this.users = users;
        this.mvc = mvc;
        this.jdbc = jdbc;
    }

    @AfterEach
    void deleteTestUser() {
        jdbc.update("DELETE FROM users WHERE subject = ?", subject);
    }

    @Test
    void M1_2_currentUserIdProvisionsOnFirstUse() {
        UUID id = users.currentUserId(token());

        UUID stored = jdbc.queryForObject("SELECT id FROM users WHERE subject = ?", UUID.class, subject);
        assertThat(id).isEqualTo(stored);
    }

    @Test
    void M1_2_currentUserIdMatchesGetMe() throws Exception {
        var me = mvc.get().uri("/api/me").with(jwt().jwt(token())).exchange();
        assertThat(me).hasStatus(HttpStatus.OK);
        UUID fromMe = UUID.fromString(JsonPath.read(me.getResponse().getContentAsString(), "$.id"));

        UUID id = users.currentUserId(token());

        assertThat(id).isEqualTo(fromMe);
    }

    private Jwt token() {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(subject)
                .claim("name", "Ada Lovelace")
                .build();
    }
}
