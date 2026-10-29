package app.crewup.crews.internal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import app.crewup.shared.Ids;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Users, crews and members inserted straight into the database for one test, and removed again by
 * {@link #cleanUp()} (TEST-8). Inserting directly lets a test set up Friends and New Guys, which no endpoint can create
 * until invites arrive in M2.
 */
final class CrewFixtures {

    /** A User with a token for it: requests made with {@link #token()} come from this User. */
    record TestUser(UUID id, String subject) {

        RequestPostProcessor token() {
            return jwt().jwt(token -> token.subject(subject).claim("name", "Test User"));
        }
    }

    private final JdbcTemplate jdbc;
    private final List<UUID> crewIds = new ArrayList<>();
    private final List<UUID> userIds = new ArrayList<>();

    CrewFixtures(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    TestUser user() {
        var user = new TestUser(Ids.newId(), "test-subject-" + Ids.newId());
        jdbc.update(
                "INSERT INTO users (id, subject, display_name, created_at, updated_at) VALUES (?, ?, ?, now(), now())",
                user.id(),
                user.subject(),
                "Test User");
        userIds.add(user.id());
        return user;
    }

    /** A crew established October 1999, with the USERS icon, in America/Chicago, at version 0. */
    UUID crew(String name) {
        UUID id = Ids.newId();
        UUID author = Ids.newId();
        jdbc.update("""
                INSERT INTO crews (id, name, established_month, icon_key, time_zone, version,
                                   created_at, created_by, updated_at, updated_by)
                VALUES (?, ?, DATE '1999-10-01', 'USERS', 'America/Chicago', 0, now(), ?, now(), ?)
                """, id, name, author, author);
        crewIds.add(id);
        return id;
    }

    UUID member(UUID crewId, TestUser user, MemberRole role) {
        UUID id = Ids.newId();
        jdbc.update("""
                INSERT INTO members (id, crew_id, user_id, role, first_name, version,
                                     created_at, created_by, updated_at, updated_by)
                VALUES (?, ?, ?, ?, 'Test', 0, now(), ?, now(), ?)
                """, id, crewId, user.id(), role.name(), user.id(), user.id());
        return id;
    }

    void softDelete(UUID crewId) {
        jdbc.update("UPDATE crews SET deleted_at = now() WHERE id = ?", crewId);
    }

    /** Removes the fixtures' crews, any crew a fixture user belongs to (such as one created through the API), and the users. */
    void cleanUp() {
        crewIds.forEach(id -> jdbc.update("DELETE FROM crews WHERE id = ?", id));
        for (UUID userId : userIds) {
            jdbc.update("DELETE FROM crews WHERE id IN (SELECT crew_id FROM members WHERE user_id = ?)", userId);
            jdbc.update("DELETE FROM users WHERE id = ?", userId);
        }
    }
}
