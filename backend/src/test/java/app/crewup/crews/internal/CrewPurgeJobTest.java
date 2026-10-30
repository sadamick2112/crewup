package app.crewup.crews.internal;

import static org.assertj.core.api.Assertions.assertThat;

import app.crewup.IntegrationTest;
import app.crewup.crews.internal.CrewFixtures.TestUser;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

// Permanent deletion of crews 30 days after they were deleted (M1-7), calling the job directly.
@IntegrationTest
class CrewPurgeJobTest {

    private final CrewPurgeJob job;
    private final JdbcTemplate jdbc;
    private final CrewFixtures fixtures;

    @Autowired
    CrewPurgeJobTest(CrewPurgeJob job, JdbcTemplate jdbc) {
        this.job = job;
        this.jdbc = jdbc;
        this.fixtures = new CrewFixtures(jdbc);
    }

    @AfterEach
    void cleanUp() {
        fixtures.cleanUp();
    }

    @Test
    void M1_7_GRP3_purgeDeletesCrewsOlderThan30DaysWithMembers() {
        TestUser user = fixtures.user();
        UUID crewId = fixtures.crew("Alpha");
        UUID memberId = fixtures.member(crewId, user, MemberRole.ADMIN);
        deletedDaysAgo(crewId, 31);

        job.purge();

        assertThat(crewExists(crewId)).isFalse();
        assertThat(memberExists(memberId)).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE id = ?", Integer.class, user.id()))
                .isOne();
    }

    @Test
    void M1_7_GRP3_purgeKeepsRecentlyDeletedCrews() {
        TestUser user = fixtures.user();
        UUID recent = fixtures.crew("Recently deleted");
        UUID memberId = fixtures.member(recent, user, MemberRole.ADMIN);
        UUID live = fixtures.crew("Not deleted");
        deletedDaysAgo(recent, 29);

        job.purge();

        assertThat(crewExists(recent)).isTrue();
        assertThat(memberExists(memberId)).isTrue();
        assertThat(crewExists(live)).isTrue();
    }

    private void deletedDaysAgo(UUID crewId, int days) {
        jdbc.update("UPDATE crews SET deleted_at = now() - make_interval(days => ?) WHERE id = ?", days, crewId);
    }

    private boolean crewExists(UUID id) {
        return jdbc.queryForObject("SELECT count(*) FROM crews WHERE id = ?", Integer.class, id) == 1;
    }

    private boolean memberExists(UUID id) {
        return jdbc.queryForObject("SELECT count(*) FROM members WHERE id = ?", Integer.class, id) == 1;
    }
}
