package app.crewup;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class SchemaMigrationTest {

    private final Flyway flyway;

    @Autowired
    SchemaMigrationTest(Flyway flyway) {
        this.flyway = flyway;
    }

    @Test
    void M0_2_applicationStartsAndMigratesSchema() {
        var migrations = flyway.info();

        assertThat(migrations.applied())
                .isNotEmpty()
                .extracting(MigrationInfo::getState)
                .allMatch(MigrationState::isApplied)
                .noneMatch(MigrationState::isFailed);
        assertThat(migrations.pending()).isEmpty();
    }
}
