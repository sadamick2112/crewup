// Fixture for slf4j-no-concat (JAVA-12, LOG-1, M0-16c): `semgrep --test .semgrep` checks every annotated line.
package fixtures;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Slf4jNoConcat {

    private static final Logger log = LoggerFactory.getLogger(Slf4jNoConcat.class);

    void concatenated(UUID eventId, RuntimeException failure) {
        // ruleid: slf4j-no-concat
        log.info("Created event " + eventId);
        // ruleid: slf4j-no-concat
        log.error("Could not create event " + eventId, failure);
        // ruleid: slf4j-no-concat
        log.atInfo().log("Created event " + eventId);
    }

    void formatted(UUID eventId) {
        // ruleid: slf4j-no-concat
        log.warn(String.format("Event %s not found", eventId));
        // ruleid: slf4j-no-concat
        log.debug("Event %s not found".formatted(eventId));
    }

    void parameterized(UUID eventId, RuntimeException failure) {
        // ok: slf4j-no-concat
        log.info("Created event {}", eventId);
        // ok: slf4j-no-concat
        log.error("Could not create event {}", eventId, failure);
        // ok: slf4j-no-concat
        log.atInfo().addKeyValue("eventId", eventId).log("Event created");
        // ok: slf4j-no-concat
        log.info("A long message that is " + "split over two lines");
    }
}
