package app.crewup.crews.internal;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Erases crews deleted more than 30 days ago, with their members through the foreign key's cascade (M1-7). One
 * statement, so running it twice, or on two app instances at once, is harmless.
 */
@Component
class CrewPurgeJob {

    static final Duration RETENTION = Duration.ofDays(30);

    private static final Logger log = LoggerFactory.getLogger(CrewPurgeJob.class);

    private final CrewRepository crews;

    CrewPurgeJob(CrewRepository crews) {
        this.crews = crews;
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "UTC")
    @Transactional
    void purge() {
        int count = crews.deleteDeletedBefore(Instant.now().minus(RETENTION));
        log.atInfo().addKeyValue("count", count).log("Deleted crews purged");
    }
}
